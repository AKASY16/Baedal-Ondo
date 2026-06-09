package com.beadalondo.api.score.calculator;

import com.beadalondo.api.score.status.DayDemandLevel;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DayWeightCalculatorTest {

    private final DayWeightCalculator calculator = new DayWeightCalculator();




    @Test
    void weekendTest() {

        //given
        LocalDate saturday = LocalDate.of(2026, 5, 30); // 토요일
        LocalDate sunday = LocalDate.of(2026, 5, 31);   // 일요일

        //when
        DayDemandLevel saturdayResult = calculator.calculate(saturday);
        DayDemandLevel sundayResult = calculator.calculate(sunday);

        //then
        assertEquals(DayDemandLevel.WEEKEND, saturdayResult);
        assertEquals(DayDemandLevel.WEEKEND, sundayResult);
    }

    @Test
    void fridayTest() {

        //given
        LocalDate friday = LocalDate.of(2026, 5, 29);   // 금요일

        //when
        DayDemandLevel fridayResult = calculator.calculate(friday);

        //then
        assertEquals(DayDemandLevel.FRIDAY, fridayResult);
    }

    @Test
    void weekdayTest() {

        //given
        LocalDate weekday = LocalDate.of(2026, 5, 28);  // 목요일

        //when
        DayDemandLevel weekdayResult = calculator.calculate(weekday);

        //then
        assertEquals(DayDemandLevel.WEEKDAY, weekdayResult);
    }

    @Test
    void weekdayAndHolidayTrueTest() {

        //given
        LocalDate weekday = LocalDate.of(2026, 5, 28);
        boolean holiday = true;

        //when
        DayDemandLevel holidayResult = calculator.calculate(weekday, holiday);

        //then
        assertEquals(DayDemandLevel.HOLIDAY, holidayResult);
    }

    @Test
    void weekdayAndHolidayFalseTest() {

        //given
        LocalDate weekday = LocalDate.of(2026, 5, 28);
        boolean holiday = false;

        //when
        DayDemandLevel holidayResult = calculator.calculate(weekday, holiday);

        //then
        assertEquals(DayDemandLevel.WEEKDAY, holidayResult);
    }
}
