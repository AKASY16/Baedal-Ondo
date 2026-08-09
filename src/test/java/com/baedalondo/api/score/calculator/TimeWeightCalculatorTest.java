package com.baedalondo.api.score.calculator;

import com.baedalondo.api.score.status.TimeDemandLevel;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;


class TimeWeightCalculatorTest {

    private final TimeWeightCalculator calculator = new TimeWeightCalculator();

    @Test
    void testTime01_0259() {

        //given
        final LocalTime testTime01 = LocalTime.of(1, 0, 0);
        final LocalTime testTime0259 = LocalTime.of(2, 59, 0);

        //when
        TimeDemandLevel startResult = calculator.calculate(testTime01);
        TimeDemandLevel endResult = calculator.calculate(testTime0259);

        //then
        assertEquals(TimeDemandLevel.LOW, startResult);
        assertEquals(TimeDemandLevel.LOW, endResult);
    }

    @Test
    void testTime03_0559() {

        //given
        final LocalTime testTime03 = LocalTime.of(3, 0, 0);
        final  LocalTime testTime0559 = LocalTime.of(5, 59, 0);

        //when
        TimeDemandLevel startResult = calculator.calculate(testTime03);
        TimeDemandLevel endResult = calculator.calculate(testTime0559);

        //then
        assertEquals(TimeDemandLevel.CLOSED, startResult);
        assertEquals(TimeDemandLevel.CLOSED, endResult);
    }

    @Test
    void testTime06_1029() {

        //given
        final LocalTime testTime06 = LocalTime.of(6, 0, 0);
        final LocalTime testTime1029 = LocalTime.of(10, 29, 0);

        //when
        TimeDemandLevel startResult = calculator.calculate(testTime06);
        TimeDemandLevel endResult = calculator.calculate(testTime1029);

        //then
        assertEquals(TimeDemandLevel.LOW, startResult);
        assertEquals(TimeDemandLevel.LOW, endResult);
    }

    @Test
    void testTime1030_1259() {

        //given
        final LocalTime testTime1030 = LocalTime.of(10, 30, 0);
        final LocalTime testTime1259 = LocalTime.of(12, 59, 0);

        //when
        TimeDemandLevel startResult = calculator.calculate(testTime1030);
        TimeDemandLevel endResult = calculator.calculate(testTime1259);

        //then
        assertEquals(TimeDemandLevel.HIGH, startResult);
        assertEquals(TimeDemandLevel.HIGH, endResult);
    }

    @Test
    void testTime13_1659() {

        //given
        final LocalTime testTime13 = LocalTime.of(13, 0, 0);
        final  LocalTime testTime1659 = LocalTime.of(16, 59, 0);

        //when
        TimeDemandLevel startResult = calculator.calculate(testTime13);
        TimeDemandLevel endResult = calculator.calculate(testTime1659);

        //then
        assertEquals(TimeDemandLevel.MEDIUM, startResult);
        assertEquals(TimeDemandLevel.MEDIUM, endResult);
    }

    @Test
    void testTime17_1959() {

        //given
        final LocalTime testTime17 = LocalTime.of(17, 0, 0);
        final LocalTime testTime1959 = LocalTime.of(19, 59, 0);

        //when
        TimeDemandLevel startResult = calculator.calculate(testTime17);
        TimeDemandLevel endResult = calculator.calculate(testTime1959);

        //then
        assertEquals(TimeDemandLevel.VERY_HIGH, startResult);
        assertEquals(TimeDemandLevel.VERY_HIGH, endResult);
    }

    @Test
    void testTime20_2259() {

        //given
        final LocalTime testTime20 = LocalTime.of(20, 0, 0);
        final LocalTime testTime2259 = LocalTime.of(22, 59, 0);

        //when
        TimeDemandLevel startRresult = calculator.calculate(testTime20);
        TimeDemandLevel endResult = calculator.calculate(testTime2259);

        //then
        assertEquals(TimeDemandLevel.HIGH, startRresult);
        assertEquals(TimeDemandLevel.HIGH, endResult);
    }

    @Test
    void testTime23_0059() {

        //given
        final LocalTime testTime23 = LocalTime.of(23, 0, 0);
        final LocalTime testTime0059 = LocalTime.of(00, 59, 0);

        //when
        TimeDemandLevel startResult = calculator.calculate(testTime23);
        TimeDemandLevel endResult = calculator.calculate(testTime0059);

        //then
        assertEquals(TimeDemandLevel.MEDIUM, startResult);
        assertEquals(TimeDemandLevel.MEDIUM, endResult);
    }
}
