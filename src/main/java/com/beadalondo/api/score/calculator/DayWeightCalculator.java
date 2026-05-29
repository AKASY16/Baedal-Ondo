package com.beadalondo.api.score.calculator;

import com.beadalondo.api.score.status.DayDemandLevel;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Component
public class DayWeightCalculator {

    public DayDemandLevel calculate(LocalDate date, boolean holiday) {
        if (holiday) {
            return DayDemandLevel.HOLIDAY;
        }

        return calculate(date);
    }

    public DayDemandLevel calculate(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return DayDemandLevel.WEEKEND;
        }

        if (dayOfWeek == DayOfWeek.FRIDAY) {
            return DayDemandLevel.FRIDAY;
        }

        return DayDemandLevel.WEEKDAY;

    }

}
