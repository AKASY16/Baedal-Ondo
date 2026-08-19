package com.baedalondo.api.holiday.service;

import com.baedalondo.api.common.ServiceTime;
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
    public void refreshHolidaysForMonthAndNextMonth(int year, int month) {

        int nextYear = year;
        int nextMonth = month + 1;

        if (month == 12) {
            nextMonth = 1;
            nextYear++;
        }

        List<Holiday> holidays = holidayClient.fetchHolidays(year, month);
        List<Holiday> holidaysNextMonth =
                holidayClient.fetchHolidays(nextYear, nextMonth);

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate startDateNextMonth = LocalDate.of(nextYear, nextMonth, 1);

        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        LocalDate endDateNextMonth =
                startDateNextMonth.withDayOfMonth(startDateNextMonth.lengthOfMonth());


        holidayRepository.deleteByDateBetween(startDate, endDate);
        holidayRepository.deleteByDateBetween(startDateNextMonth, endDateNextMonth);
        holidayRepository.flush();

        Map<LocalDate, Holiday> holidaysByDate = new LinkedHashMap<>();

        for (Holiday holiday : holidays) {
            holidaysByDate.putIfAbsent(holiday.getDate(), holiday);
        }

        for (Holiday holiday : holidaysNextMonth) {
            holidaysByDate.putIfAbsent(holiday.getDate(), holiday);
        }

        LocalDate date = startDate;

        while (!date.isAfter(endDateNextMonth)) {

            holidaysByDate.putIfAbsent(
                    date,
                    new Holiday(
                            date,
                            "비공휴일",
                            null,
                            false,
                            null
                    )
            );

            date = date.plusDays(1);
        }

        holidayRepository.saveAll(holidaysByDate.values());
    }



    /**
     서버가 뜨면 이번 달과 다음 달을 채운다. 스케줄러가 매일 6시에 도는 것과 같은 범위다.

     연 단위로 채우던 것을 바꿨다. 연 단위 갱신은 그 해 전체를 지우고 실제 공휴일만 저장해서
     비공휴일 행을 전부 날려 버렸고, 그러면 재시작 직후 첫 요청이 다시 외부 API를 호출했다.
     트랜잭션을 연 채로 API를 12번 부르는 문제도 함께 사라진다.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void refreshHolidaysOnStartup() {
        if (!startupRefreshEnabled) {
            log.info("서버 시작 시 공휴일 갱신을 건너뜁니다.");
            return;
        }

        // 서버 시간대와 무관하게 한국 기준 날짜를 써야 한다.
        // UTC 서버에서는 자정 직후에 아직 전날로 계산되어 월이 어긋난다.
        LocalDate today = ServiceTime.today();
        int year = today.getYear();
        int month = today.getMonthValue();

        try {
            transactionTemplate.executeWithoutResult(
                    status -> refreshHolidaysForMonthAndNextMonth(year, month));
        } catch (RuntimeException e) {
            log.warn("서버 시작 시 공휴일 갱신에 실패했습니다. year={}, month={}", year, month, e);
        }
    }

    private boolean refreshMonthAndCheck(LocalDate date) {
        refreshHolidaysForMonthAndNextMonth(
                date.getYear(),
                date.getMonthValue()
        );

        return holidayRepository.findByDate(date)
                .map(Holiday::getHoliday)
                .map(Boolean.TRUE::equals)
                .orElse(false);
    }

    private static final Logger log = LoggerFactory.getLogger(HolidayService.class);
}
