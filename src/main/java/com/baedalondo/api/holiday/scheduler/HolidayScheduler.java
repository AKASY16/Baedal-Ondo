package com.baedalondo.api.holiday.scheduler;

import com.baedalondo.api.common.ServiceTime;
import com.baedalondo.api.holiday.client.HolidayClient;
import com.baedalondo.api.holiday.entity.Holiday;
import com.baedalondo.api.holiday.repository.HolidayRepository;
import com.baedalondo.api.holiday.service.HolidayService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
