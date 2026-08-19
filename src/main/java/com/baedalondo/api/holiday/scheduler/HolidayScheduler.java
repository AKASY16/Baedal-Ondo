package com.baedalondo.api.holiday.scheduler;

import com.baedalondo.api.common.ServiceTime;
import com.baedalondo.api.holiday.service.HolidayService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class HolidayScheduler {

    private final HolidayService holidayService;

    public HolidayScheduler(HolidayService holidayService) {
        this.holidayService = holidayService;
    }

    @Scheduled(
            cron = "0 0 6 * * *",
            zone = "Asia/Seoul")
    public void refreshHolidays() {

        LocalDate today = ServiceTime.today();

        int year = today.getYear();
        int month = today.getMonthValue();

        holidayService.refreshHolidaysForMonthAndNextMonth(year, month);
    }

}
