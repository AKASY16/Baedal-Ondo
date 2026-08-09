package com.baedalondo.api.score.calculator;

import com.baedalondo.api.score.status.TimeDemandLevel;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
public class TimeWeightCalculator {

    public TimeDemandLevel calculate(LocalTime nowTime) {
        LocalTime t01 = LocalTime.of(1, 0);
        LocalTime t03 = LocalTime.of(3, 0);
        LocalTime t06 = LocalTime.of(6, 0);
        LocalTime t1030 = LocalTime.of(10, 30);
        LocalTime t13 = LocalTime.of(13, 0);
        LocalTime t17 = LocalTime.of(17, 0);
        LocalTime t20 = LocalTime.of(20, 0);
        LocalTime t23 = LocalTime.of(23, 0);

        if (isBetween(nowTime, t03, t06)) {
            return TimeDemandLevel.CLOSED;
        }

        if (isBetween(nowTime, t06, t1030)) {
            return TimeDemandLevel.LOW;
        }

        if (isBetween(nowTime, t1030, t13)) {
            return TimeDemandLevel.HIGH;
        }

        if (isBetween(nowTime, t13, t17)) {
            return TimeDemandLevel.MEDIUM;
        }

        if (isBetween(nowTime, t17, t20)) {
            return TimeDemandLevel.VERY_HIGH;
        }

        if (isBetween(nowTime, t20, t23)) {
            return TimeDemandLevel.HIGH;
        }

        if (isBetweenAcrossMidnight(nowTime, t23, t01)) {
            return TimeDemandLevel.MEDIUM;
        }

        if (isBetween(nowTime, t01, t03)) {
            return TimeDemandLevel.LOW;
        }

        return TimeDemandLevel.CLOSED;
    }

    private boolean isBetween(LocalTime target, LocalTime start, LocalTime end) {
        return !target.isBefore(start) && target.isBefore(end);
    }

    private boolean isBetweenAcrossMidnight(LocalTime target, LocalTime start, LocalTime end) {
        return !target.isBefore(start) || target.isBefore(end);
    }
}