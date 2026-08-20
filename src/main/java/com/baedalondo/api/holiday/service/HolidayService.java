package com.baedalondo.api.holiday.service;

import com.baedalondo.api.common.ExternalCallGuard;
import com.baedalondo.api.holiday.client.HolidayClient;
import com.baedalondo.api.holiday.entity.Holiday;
import com.baedalondo.api.holiday.repository.HolidayRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class HolidayService {

    private final HolidayClient holidayClient;
    private final HolidayRepository holidayRepository;
    private final ExternalCallGuard externalCallGuard;

    public HolidayService(HolidayClient holidayClient,
                          HolidayRepository holidayRepository,
                          ExternalCallGuard externalCallGuard) {
        this.holidayClient = holidayClient;
        this.holidayRepository = holidayRepository;
        this.externalCallGuard = externalCallGuard;
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

        List<Holiday> holidays = fetchHolidays(year, month);
        List<Holiday> holidaysNextMonth = fetchHolidays(nextYear, nextMonth);

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
     달 하나가 한 번의 호출로 채워지므로 연·월이 곧 조회 단위다.

     쿨다운 중이면 예외를 던진다. 여기서 빈 목록을 돌려주면 그 달 전체가 비공휴일로 저장되어,
     외부 API가 잠깐 흔들린 것이 "공휴일이 없는 달"로 굳어 버린다.
     예외를 던지면 트랜잭션이 롤백되어 기존 데이터가 그대로 남는다.
     */
    private List<Holiday> fetchHolidays(int year, int month) {
        String cooldownKey = cooldownKey(year, month);

        if (externalCallGuard.isCoolingDown(cooldownKey)) {
            throw new IllegalStateException(
                    "공휴일 조회가 연속 실패해 잠시 호출을 멈춘 상태입니다. year=" + year + ", month=" + month);
        }

        return externalCallGuard.call(cooldownKey, () -> holidayClient.fetchHolidays(year, month));
    }

    private String cooldownKey(int year, int month) {
        return "holiday:" + year + ":" + month;
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
}
