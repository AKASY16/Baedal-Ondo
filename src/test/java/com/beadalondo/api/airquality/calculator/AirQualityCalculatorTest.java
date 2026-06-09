package com.beadalondo.api.airquality.calculator;

import com.beadalondo.api.airquality.domain.CurrentAirQualityObservation;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AirQualityCalculatorTest {

    private final AirQualityCalculator calculator =
            new AirQualityCalculator();


    @Test
    void getWeightNullTest() {
        calculator.getWeight(null);

        assertEquals(0, calculator.getWeight(null));
    }

    @Test
    void getWeightPm10Test() {
        // given
        LocalDateTime date = LocalDateTime.parse("2026-05-31T15:30:00");

        CurrentAirQualityObservation observationPm10_80 = new CurrentAirQualityObservation(
                "서울",
                "성동구",
                "1",
                "도시대기",
                date,
                80,
                null,
                null,
                0,
                0,
                0,
                0,
                0,
                0,
                0
        );

        CurrentAirQualityObservation observationPm10_81 = new CurrentAirQualityObservation(
                "서울",
                "성동구",
                "1",
                "도시대기",
                date,
                81,
                null,
                null,
                0,
                0,
                0,
                0,
                0,
                0,
                0
        );

        CurrentAirQualityObservation observationPm10_150 = new CurrentAirQualityObservation(
                "서울",
                "성동구",
                "1",
                "도시대기",
                date,
                150,
                null,
                null,
                0,
                0,
                0,
                0,
                0,
                0,
                0
        );

        CurrentAirQualityObservation observationPm10_151 = new CurrentAirQualityObservation(
                "서울",
                "성동구",
                "1",
                "도시대기",
                date,
                151,
                null,
                null,
                0,
                0,
                0,
                0,
                0,
                0,
                0
        );

        //when
        int pm10_80Score = calculator.getWeight(observationPm10_80);
        int pm10_81Score = calculator.getWeight(observationPm10_81);
        int pm10_150Score = calculator.getWeight(observationPm10_150);
        int pm10_151Score = calculator.getWeight(observationPm10_151);

        //then
        assertEquals(0, pm10_80Score);
        assertEquals(1, pm10_81Score);
        assertEquals(1, pm10_150Score);
        assertEquals(2, pm10_151Score);

    }

    @Test
    void getWeightPm25Test() {
        // given
        LocalDateTime date = LocalDateTime.parse("2026-05-31T15:30:00");

        CurrentAirQualityObservation observationPm25_35 = new CurrentAirQualityObservation(
                "서울",
                "성동구",
                "1",
                "도시대기",
                date,
                null,
                35,
                null,
                0,
                0,
                0,
                0,
                0,
                0,
                0
        );

        CurrentAirQualityObservation observationPm25_36 = new CurrentAirQualityObservation(
                "서울",
                "성동구",
                "1",
                "도시대기",
                date,
                null,
                36,
                null,
                0,
                0,
                0,
                0,
                0,
                0,
                0
        );

        CurrentAirQualityObservation observationPm25_75 = new CurrentAirQualityObservation(
                "서울",
                "성동구",
                "1",
                "도시대기",
                date,
                null,
                75,
                null,
                0,
                0,
                0,
                0,
                0,
                0,
                0
        );

        CurrentAirQualityObservation observationPm25_76 = new CurrentAirQualityObservation(
                "서울",
                "성동구",
                "1",
                "도시대기",
                date,
                null,
                76,
                null,
                0,
                0,
                0,
                0,
                0,
                0,
                0
        );

        //when
        int pm25_35Score = calculator.getWeight(observationPm25_35);
        int pm25_36Score = calculator.getWeight(observationPm25_36);
        int pm25_75Score = calculator.getWeight(observationPm25_75);
        int pm25_76Score = calculator.getWeight(observationPm25_76);

        //then
        assertEquals(0, pm25_35Score);
        assertEquals(2, pm25_36Score);
        assertEquals(2, pm25_75Score);
        assertEquals(3, pm25_76Score);

    }

    @Test
    void getWeightO3Test() {
        // given
        LocalDateTime date = LocalDateTime.parse("2026-05-31T15:30:00");

        CurrentAirQualityObservation observationO3_0_090 = new CurrentAirQualityObservation(
                "서울",
                "성동구",
                "1",
                "도시대기",
                date,
                null,
                null,
                0.090,
                0,
                0,
                0,
                0,
                0,
                0,
                0
        );

        CurrentAirQualityObservation observationO3_0_091 = new CurrentAirQualityObservation(
                "서울",
                "성동구",
                "1",
                "도시대기",
                date,
                null,
                null,
                0.091,
                0,
                0,
                0,
                0,
                0,
                0,
                0
        );

        CurrentAirQualityObservation observationO3_0_150 = new CurrentAirQualityObservation(
                "서울",
                "성동구",
                "1",
                "도시대기",
                date,
                null,
                null,
                0.150,
                0,
                0,
                0,
                0,
                0,
                0,
                0
        );

        CurrentAirQualityObservation observationO3_0_151 = new CurrentAirQualityObservation(
                "서울",
                "성동구",
                "1",
                "도시대기",
                date,
                null,
                null,
                0.151,
                0,
                0,
                0,
                0,
                0,
                0,
                0
        );

        //when
        int o3_0_090Score = calculator.getWeight(observationO3_0_090);
        int o3_0_091Score = calculator.getWeight(observationO3_0_091);
        int o3_0_150Score = calculator.getWeight(observationO3_0_150);
        int o3_0_151Score = calculator.getWeight(observationO3_0_151);

        //then
        assertEquals(0, o3_0_090Score);
        assertEquals(1, o3_0_091Score);
        assertEquals(1, o3_0_150Score);
        assertEquals(2, o3_0_151Score);

    }
}
