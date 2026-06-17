package com.beadalondo.api.score.calculator;

import com.beadalondo.api.score.ScoreBreakdown;
import com.beadalondo.api.score.status.DayDemandLevel;
import com.beadalondo.api.score.status.TimeDemandLevel;
import com.beadalondo.api.weather.domain.CurrentWeatherObservation;
import com.beadalondo.api.weather.domain.WeatherScoreResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeightedScoreCalculatorTest {

    private final WeightedScoreCalculator calculator = new WeightedScoreCalculator();

    @Test
    void mediumWeekdayWithoutWeatherOrAirQualityStartsAtAverageScore() {
        ScoreBreakdown result = calculator.calculate(
                TimeDemandLevel.MEDIUM,
                DayDemandLevel.WEEKDAY,
                new WeatherScoreResult(0, List.of(), "날씨 영향 없음"),
                noRainWeather(),
                0
        );

        assertEquals(50, result.score());
        assertEquals(0, result.timeScore());
        assertEquals(0, result.dayScore());
        assertEquals(0, result.weatherScore());
        assertEquals(0, result.airQualityScore());
        assertEquals(0, result.interactionScore());
    }

    @Test
    void lowAndClosedTimeCanLowerScoreBelowAverage() {
        ScoreBreakdown low = calculator.calculate(
                TimeDemandLevel.LOW,
                DayDemandLevel.WEEKDAY,
                new WeatherScoreResult(0, List.of(), "날씨 영향 없음"),
                noRainWeather(),
                0
        );
        ScoreBreakdown closed = calculator.calculate(
                TimeDemandLevel.CLOSED,
                DayDemandLevel.WEEKDAY,
                new WeatherScoreResult(0, List.of(), "날씨 영향 없음"),
                noRainWeather(),
                0
        );

        assertEquals(42, low.score());
        assertEquals(32, closed.score());
    }

    @Test
    void rawWeatherAndAirQualityScoresAreNormalizedToWeightedRanges() {
        ScoreBreakdown result = calculator.calculate(
                TimeDemandLevel.MEDIUM,
                DayDemandLevel.WEEKDAY,
                new WeatherScoreResult(17, List.of("강수량", "기온"), "강수량, 기온"),
                rainyWeather(),
                7
        );

        assertEquals(78, result.score());
        assertEquals(20, result.weatherScore());
        assertEquals(8, result.airQualityScore());
    }

    @Test
    void peakWeekendRainAddsInteractionBonusWithCap() {
        ScoreBreakdown result = calculator.calculate(
                TimeDemandLevel.VERY_HIGH,
                DayDemandLevel.WEEKEND,
                new WeatherScoreResult(5, List.of("강수량"), "강수량"),
                rainyWeather(),
                0
        );

        assertEquals(98, result.score());
        assertEquals(24, result.timeScore());
        assertEquals(8, result.dayScore());
        assertEquals(6, result.weatherScore());
        assertEquals(10, result.interactionScore());
    }

    @Test
    void finalScoreIsCappedAt100() {
        ScoreBreakdown result = calculator.calculate(
                TimeDemandLevel.VERY_HIGH,
                DayDemandLevel.WEEKEND,
                new WeatherScoreResult(100, List.of("악천후"), "악천후"),
                rainyWeather(),
                100
        );

        assertEquals(100, result.score());
    }

    @Test
    void closedTimeCannotRiseAboveLowDemandRange() {
        ScoreBreakdown result = calculator.calculate(
                TimeDemandLevel.CLOSED,
                DayDemandLevel.HOLIDAY,
                new WeatherScoreResult(17, List.of("악천후"), "악천후"),
                rainyWeather(),
                7
        );

        assertEquals(39, result.score());
    }

    @Test
    void lowTimeCannotRiseAboveAverageDemandRange() {
        ScoreBreakdown result = calculator.calculate(
                TimeDemandLevel.LOW,
                DayDemandLevel.HOLIDAY,
                new WeatherScoreResult(17, List.of("악천후"), "악천후"),
                rainyWeather(),
                7
        );

        assertEquals(59, result.score());
    }

    @Test
    void mediumTimeCannotRiseAboveHighDemandRange() {
        ScoreBreakdown result = calculator.calculate(
                TimeDemandLevel.MEDIUM,
                DayDemandLevel.HOLIDAY,
                new WeatherScoreResult(17, List.of("악천후"), "악천후"),
                rainyWeather(),
                7
        );

        assertEquals(79, result.score());
    }

    private CurrentWeatherObservation noRainWeather() {
        return new CurrentWeatherObservation(0, 0, 20, 50, 1);
    }

    private CurrentWeatherObservation rainyWeather() {
        return new CurrentWeatherObservation(1, 5, 20, 80, 3);
    }
}
