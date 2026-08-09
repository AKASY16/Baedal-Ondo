package com.baedalondo.api.holiday.service;

import com.baedalondo.api.holiday.client.HolidayClient;
import com.baedalondo.api.holiday.entity.Holiday;
import com.baedalondo.api.holiday.repository.HolidayRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class HolidayService {

    private final HolidayClient holidayClient;
    private final HolidayRepository holidayRepository;
    private final TransactionTemplate transactionTemplate;
    private final boolean startupRefreshEnabled;

    public HolidayService(HolidayClient holidayClient,
                          HolidayRepository holidayRepository,
                          TransactionTemplate transactionTemplate,
                          @Value("${kasi.api.startup-refresh-enabled:true}") boolean startupRefreshEnabled) {
        this.holidayClient = holidayClient;
        this.holidayRepository = holidayRepository;
        this.transactionTemplate = transactionTemplate;
        this.startupRefreshEnabled = startupRefreshEnabled;
    }

    @Transactional
    public boolean isHoliday(LocalDate date) {
        return holidayRepository.findByDate(date)
                .map(Holiday::getHoliday)
                .map(Boolean.TRUE::equals)
                .orElseGet(() -> refreshMonthAndCheck(date));
    }

    @Transactional
    public void refreshHolidaysForYear(int year) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        holidayRepository.deleteByDateBetween(startDate, endDate);
        holidayRepository.flush();

        Map<LocalDate, Holiday> holidaysByDate = new LinkedHashMap<>();
        for (int month = 1; month <= 12; month++) {
            for (Holiday holiday : holidayClient.fetchHolidays(year, month)) {
                holidaysByDate.putIfAbsent(holiday.getDate(), holiday);
            }
        }

        holidayRepository.saveAll(holidaysByDate.values());
        log.info("공휴일 연도 갱신 완료: year={}, count={}", year, holidaysByDate.size());
    }

    @Transactional
    public void refreshHolidaysForMonth(int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        holidayRepository.deleteByDateBetween(startDate, endDate);
        holidayRepository.flush();

        List<Holiday> holidays = holidayClient.fetchHolidays(year, month);
        Map<LocalDate, Holiday> holidaysByDate = new LinkedHashMap<>();
        for (Holiday holiday : holidays) {
            holidaysByDate.putIfAbsent(holiday.getDate(), holiday);
        }

        holidayRepository.saveAll(holidaysByDate.values());
        log.info("공휴일 월 갱신 완료: year={}, month={}, count={}", year, month, holidaysByDate.size());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void refreshCurrentYearHolidaysOnStartup() {
        if (!startupRefreshEnabled) {
            log.info("서버 시작 시 공휴일 갱신을 건너뜁니다.");
            return;
        }

        int currentYear = LocalDate.now().getYear();
        try {
            transactionTemplate.executeWithoutResult(status -> refreshHolidaysForYear(currentYear));
        } catch (RuntimeException e) {
            log.warn("서버 시작 시 공휴일 갱신에 실패했습니다. year={}", currentYear, e);
        }
    }

    private boolean refreshMonthAndCheck(LocalDate date) {
        refreshHolidaysForMonth(date.getYear(), date.getMonthValue());

        return holidayRepository.findByDate(date)
                .map(Holiday::getHoliday)
                .map(Boolean.TRUE::equals)
                .orElse(false);
    }

    private static final Logger log = LoggerFactory.getLogger(HolidayService.class);
}
