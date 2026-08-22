package com.baedalondo.api.airquality.calculator;

import com.baedalondo.api.airquality.domain.CurrentAirQualityObservation;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AirQualityCalculatorTest {

    private final AirQualityCalculator calculator = new AirQualityCalculator();

    @Test
    void getWeightNullTest() {
        assertEquals(0, calculator.getWeight(null));
    }

    @Test
    void getWeightPm10Test() {
        assertEquals(0, calculator.getWeight(observation(80, null)));
        assertEquals(1, calculator.getWeight(observation(81, null)));
        assertEquals(1, calculator.getWeight(observation(150, null)));
        assertEquals(2, calculator.getWeight(observation(151, null)));
    }

    @Test
    void getWeightPm25Test() {
        assertEquals(0, calculator.getWeight(observation(null, 35)));
        assertEquals(2, calculator.getWeight(observation(null, 36)));
        assertEquals(2, calculator.getWeight(observation(null, 75)));
        assertEquals(3, calculator.getWeight(observation(null, 76)));
    }

    @Test
    void getWeightMaximumPmValuesTest() {
        assertEquals(5, calculator.getWeight(observation(151, 76)));
    }

    @Test
    void calculatesBaseTimeFromReferenceTimeAroundTwentyMinuteBoundary() {
        assertEquals(
                LocalDateTime.of(2026, 8, 22, 13, 0),
                calculator.getSafeAirQualityBaseTime(
                        LocalDateTime.of(2026, 8, 22, 14, 19, 59))
        );
        assertEquals(
                LocalDateTime.of(2026, 8, 22, 14, 0),
                calculator.getSafeAirQualityBaseTime(
                        LocalDateTime.of(2026, 8, 22, 14, 20))
        );
    }

    private CurrentAirQualityObservation observation(Integer pm10Value, Integer pm25Value) {
        return new CurrentAirQualityObservation(
                "서울",
                "성동구",
                "1",
                "도시대기",
                LocalDateTime.parse("2026-05-31T15:30:00"),
                pm10Value,
                pm25Value,
                0,
                0,
                0,
                0,
                0,
                0
        );
    }
}
