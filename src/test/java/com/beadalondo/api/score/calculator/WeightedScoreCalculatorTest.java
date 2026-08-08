package com.beadalondo.api.score.calculator;

import com.beadalondo.api.score.ScoreCalculationResult;
import com.beadalondo.api.score.status.DayDemandLevel;
import com.beadalondo.api.score.status.TimeDemandLevel;
import com.beadalondo.api.weather.domain.CurrentWeatherObservation;
import com.beadalondo.api.weather.domain.WeatherScoreResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeightedScoreCalculatorTest {

    private static final int NO_MARKET_DAY_WEIGHT = 0;

    private final WeightedScoreCalculator calculator = new WeightedScoreCalculator();

    @Test
    void mediumWeekdayWithoutWeatherOrAirQualityStartsAtAverageScore() {
        ScoreCalculationResult result = calculator.calculate(
                TimeDemandLevel.MEDIUM,
                DayDemandLevel.WEEKDAY,
                NO_MARKET_DAY_WEIGHT,
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
        ScoreCalculationResult low = calculator.calculate(
                TimeDemandLevel.LOW,
                DayDemandLevel.WEEKDAY,
                NO_MARKET_DAY_WEIGHT,
                new WeatherScoreResult(0, List.of(), "날씨 영향 없음"),
                noRainWeather(),
                0
        );
        ScoreCalculationResult closed = calculator.calculate(
                TimeDemandLevel.CLOSED,
                DayDemandLevel.WEEKDAY,
                NO_MARKET_DAY_WEIGHT,
                new WeatherScoreResult(0, List.of(), "날씨 영향 없음"),
                noRainWeather(),
                0
        );

        assertEquals(42, low.score());
        assertEquals(32, closed.score());
    }

    @Test
    void rawWeatherAndAirQualityScoresAreNormalizedToWeightedRanges() {
        ScoreCalculationResult result = calculator.calculate(
                TimeDemandLevel.MEDIUM,
                DayDemandLevel.WEEKDAY,
                NO_MARKET_DAY_WEIGHT,
                new WeatherScoreResult(17, List.of("강수량", "기온"), "강수량, 기온"),
                rainyWeather(),
                7
        );

        assertEquals(78, result.score());
        assertEquals(20, result.weatherScore());
        assertEquals(8, result.airQualityScore());
    }

    @Test
    void peakWeekendRainAddsInteractionBonus() {
        ScoreCalculationResult result = calculator.calculate(
                TimeDemandLevel.VERY_HIGH,
                DayDemandLevel.WEEKEND,
                6,
                new WeatherScoreResult(5, List.of("강수량"), "강수량"),
                rainyWeather(),
                0
        );

        assertEquals(94, result.score());
        assertEquals(24, result.timeScore());
        assertEquals(6, result.dayScore());
        assertEquals(6, result.weatherScore());
        // 강한 요일 x 피크 +3, 비 x VERY_HIGH +5
        assertEquals(8, result.interactionScore());
    }

    @Test
    void finalScoreIsCappedAt100() {
        ScoreCalculationResult result = calculator.calculate(
                TimeDemandLevel.VERY_HIGH,
                DayDemandLevel.WEEKEND,
                6,
                new WeatherScoreResult(100, List.of("악천후"), "악천후"),
                rainyWeather(),
                100
        );

        assertEquals(100, result.score());
    }

    @Test
    void closedTimeCannotRiseAboveLowDemandRange() {
        ScoreCalculationResult result = calculator.calculate(
                TimeDemandLevel.CLOSED,
                DayDemandLevel.HOLIDAY,
                NO_MARKET_DAY_WEIGHT,
                new WeatherScoreResult(17, List.of("악천후"), "악천후"),
                rainyWeather(),
                7
        );

        assertEquals(39, result.score());
    }

    @Test
    void lowTimeCannotRiseAboveAverageDemandRange() {
        ScoreCalculationResult result = calculator.calculate(
                TimeDemandLevel.LOW,
                DayDemandLevel.HOLIDAY,
                NO_MARKET_DAY_WEIGHT,
                new WeatherScoreResult(17, List.of("악천후"), "악천후"),
                rainyWeather(),
                7
        );

        assertEquals(59, result.score());
    }

    @Test
    void mediumTimeCannotRiseAboveHighDemandRange() {
        ScoreCalculationResult result = calculator.calculate(
                TimeDemandLevel.MEDIUM,
                DayDemandLevel.HOLIDAY,
                NO_MARKET_DAY_WEIGHT,
                new WeatherScoreResult(17, List.of("악천후"), "악천후"),
                rainyWeather(),
                7
        );

        assertEquals(79, result.score());
    }

    // ------------------------------------------------ 상권별 DayWeight 적용

    @Test
    @DisplayName("Local +6이 요일 점수 +6으로 그대로 적용된다")
    void appliesPositiveMarketDayWeight() {
        ScoreCalculationResult result = calmMediumDay(DayDemandLevel.WEEKDAY, 6);

        assertEquals(6, result.dayScore());
        assertEquals(56, result.score());
    }

    @Test
    @DisplayName("Local -6이 요일 점수 -6으로 그대로 적용된다")
    void appliesNegativeMarketDayWeight() {
        ScoreCalculationResult result = calmMediumDay(DayDemandLevel.WEEKDAY, -6);

        assertEquals(-6, result.dayScore());
        assertEquals(44, result.score());
    }

    @Test
    @DisplayName("금요일이라는 이유로 기존 +6이 붙지 않는다")
    void fridayNoLongerAddsFixedScore() {
        ScoreCalculationResult result = calmMediumDay(DayDemandLevel.FRIDAY, NO_MARKET_DAY_WEIGHT);

        assertEquals(0, result.dayScore());
        assertEquals(50, result.score());
    }

    @Test
    @DisplayName("주말이라는 이유로 기존 +8이 붙지 않는다")
    void weekendNoLongerAddsFixedScore() {
        ScoreCalculationResult result = calmMediumDay(DayDemandLevel.WEEKEND, NO_MARKET_DAY_WEIGHT);

        assertEquals(0, result.dayScore());
        assertEquals(50, result.score());
    }

    @Test
    @DisplayName("주말에도 상권 가중치가 음수면 요일 점수가 음수가 된다")
    void weekendCanBeNegative() {
        ScoreCalculationResult result = calmMediumDay(DayDemandLevel.WEEKEND, -6);

        assertEquals(-6, result.dayScore());
        assertEquals(44, result.score());
    }

    // ------------------------------------------------ 공휴일

    @Test
    @DisplayName("공휴일은 기존 +8을 유지한다")
    void holidayKeepsFixedScore() {
        ScoreCalculationResult result = calmMediumDay(DayDemandLevel.HOLIDAY, NO_MARKET_DAY_WEIGHT);

        assertEquals(8, result.dayScore());
        assertEquals(58, result.score());
    }

    @Test
    @DisplayName("공휴일에는 상권 DayWeight를 더하지 않는다")
    void holidayIgnoresMarketDayWeight() {
        ScoreCalculationResult added = calmMediumDay(DayDemandLevel.HOLIDAY, 6);
        ScoreCalculationResult subtracted = calmMediumDay(DayDemandLevel.HOLIDAY, -6);

        // 더했다면 14, 대체했다면 6이 나온다. 둘 다 아니어야 한다.
        assertEquals(8, added.dayScore());
        assertEquals(8, subtracted.dayScore());
        assertEquals(58, added.score());
        assertEquals(58, subtracted.score());
    }

    // ------------------------------------------------ interaction

    // ------------------------------------------------ 강한 요일 x 피크 interaction

    @Test
    @DisplayName("DayWeight가 양수일 때만 피크 interaction이 붙는다")
    void dayPeakInteractionScalesWithMarketDayWeight() {
        assertEquals(0, peakInteraction(-6));
        assertEquals(0, peakInteraction(-1));
        assertEquals(0, peakInteraction(0));
        assertEquals(1, peakInteraction(1));
        assertEquals(1, peakInteraction(2));
        assertEquals(2, peakInteraction(3));
        assertEquals(2, peakInteraction(4));
        assertEquals(3, peakInteraction(5));
        assertEquals(3, peakInteraction(6));
    }

    @Test
    @DisplayName("피크 시간대가 아니면 DayWeight가 커도 interaction이 붙지 않는다")
    void noDayPeakInteractionOutsidePeakHours() {
        assertEquals(0, calmMediumDay(DayDemandLevel.WEEKDAY, 6).interactionScore());
        assertEquals(0, calmMediumDay(DayDemandLevel.WEEKEND, 6).interactionScore());
    }

    @Test
    @DisplayName("HIGH 시간대에도 피크 interaction이 붙는다")
    void dayPeakInteractionAppliesToHighTime() {
        ScoreCalculationResult result = calculator.calculate(
                TimeDemandLevel.HIGH,
                DayDemandLevel.WEEKDAY,
                6,
                new WeatherScoreResult(0, List.of(), "날씨 영향 없음"),
                noRainWeather(),
                0
        );

        assertEquals(3, result.interactionScore());
        assertEquals(73, result.score()); // 50 + 14 + 6 + 3
    }

    @Test
    @DisplayName("금요일이어도 DayWeight가 음수면 interaction이 붙지 않는다")
    void fridayNoLongerGrantsInteraction() {
        assertEquals(0, peakInteractionOn(DayDemandLevel.FRIDAY, -3));
        // 기존에는 금요일이라는 이유만으로 +5가 붙었다.
        assertEquals(0, peakInteractionOn(DayDemandLevel.FRIDAY, 0));
    }

    @Test
    @DisplayName("주말이어도 DayWeight가 음수면 interaction이 붙지 않는다")
    void weekendNoLongerGrantsInteraction() {
        assertEquals(0, peakInteractionOn(DayDemandLevel.WEEKEND, -6));
        // 기존에는 주말이라는 이유만으로 +7이 붙어 음수 요일을 뒤집었다.
        assertEquals(0, peakInteractionOn(DayDemandLevel.WEEKEND, 0));
    }

    @Test
    @DisplayName("수요가 약한 주말 저녁은 요일 순효과가 뒤집히지 않는다")
    void negativeWeekendIsNotFlippedByInteraction() {
        ScoreCalculationResult result = calculator.calculate(
                TimeDemandLevel.VERY_HIGH,
                DayDemandLevel.WEEKEND,
                -6,
                new WeatherScoreResult(0, List.of(), "날씨 영향 없음"),
                noRainWeather(),
                0
        );

        assertEquals(-6, result.dayScore());
        assertEquals(0, result.interactionScore());
        assertEquals(68, result.score()); // 50 + 24 - 6
    }

    @Test
    @DisplayName("공휴일 저녁 interaction은 기존 +4를 유지한다")
    void holidayPeakInteractionKeepsFour() {
        ScoreCalculationResult result = calculator.calculate(
                TimeDemandLevel.VERY_HIGH,
                DayDemandLevel.HOLIDAY,
                NO_MARKET_DAY_WEIGHT,
                new WeatherScoreResult(0, List.of(), "날씨 영향 없음"),
                noRainWeather(),
                0
        );

        assertEquals(4, result.interactionScore());
        assertEquals(8, result.dayScore());
        assertEquals(86, result.score()); // 50 + 24 + 8 + 4
    }

    @Test
    @DisplayName("비 interaction은 기존 규칙 그대로다")
    void rainInteractionUnchanged() {
        ScoreCalculationResult veryHigh = calculator.calculate(
                TimeDemandLevel.VERY_HIGH,
                DayDemandLevel.WEEKDAY,
                NO_MARKET_DAY_WEIGHT,
                new WeatherScoreResult(0, List.of(), "날씨 영향 없음"),
                rainyWeather(),
                0
        );
        ScoreCalculationResult high = calculator.calculate(
                TimeDemandLevel.HIGH,
                DayDemandLevel.WEEKDAY,
                NO_MARKET_DAY_WEIGHT,
                new WeatherScoreResult(0, List.of(), "날씨 영향 없음"),
                rainyWeather(),
                0
        );

        assertEquals(5, veryHigh.interactionScore());
        assertEquals(3, high.interactionScore());
    }

    @Test
    @DisplayName("공휴일에는 DayWeight가 커도 day-peak interaction이 붙지 않는다")
    void holidayIgnoresDayPeakInteraction() {
        ScoreCalculationResult result = calculator.calculate(
                TimeDemandLevel.VERY_HIGH,
                DayDemandLevel.HOLIDAY,
                6,
                new WeatherScoreResult(0, List.of(), "날씨 영향 없음"),
                noRainWeather(),
                0
        );

        // 공휴일 x 피크 +4만 적용된다. day-peak +3이 더해졌다면 7이 나온다.
        assertEquals(4, result.interactionScore());
        assertEquals(8, result.dayScore());
        assertEquals(86, result.score()); // 50 + 24 + 8 + 4

        // DayWeight가 음수여도 공휴일 interaction은 그대로 +4다.
        assertEquals(4, peakInteractionOn(DayDemandLevel.HOLIDAY, -6));
    }

    @Test
    @DisplayName("interaction 합계는 여전히 +10을 넘지 않는다")
    void interactionIsStillCappedAtTen() {
        // 공휴일 x 피크 +4, 비 x VERY_HIGH +5, 비 x 공휴일 +4 = 13
        ScoreCalculationResult holiday = calculator.calculate(
                TimeDemandLevel.VERY_HIGH,
                DayDemandLevel.HOLIDAY,
                6,
                new WeatherScoreResult(0, List.of(), "날씨 영향 없음"),
                rainyWeather(),
                0
        );

        // 일반 날짜에서도 상한이 걸리는지 확인. day-peak +3, 비 x VERY_HIGH +5 = 8
        ScoreCalculationResult weekday = calculator.calculate(
                TimeDemandLevel.VERY_HIGH,
                DayDemandLevel.WEEKDAY,
                6,
                new WeatherScoreResult(0, List.of(), "날씨 영향 없음"),
                rainyWeather(),
                0
        );

        assertEquals(10, holiday.interactionScore());
        assertEquals(8, weekday.interactionScore());
    }

    @Test
    @DisplayName("요일 점수가 음수여도 비 오는 공휴일 interaction은 그대로다")
    void rainyHolidayInteractionUnchanged() {
        ScoreCalculationResult result = calculator.calculate(
                TimeDemandLevel.VERY_HIGH,
                DayDemandLevel.HOLIDAY,
                -6,
                new WeatherScoreResult(0, List.of(), "날씨 영향 없음"),
                rainyWeather(),
                0
        );

        // 저녁 피크 x 공휴일 4 + 비 x VERY_HIGH 5 + 비 x 공휴일 4 = 13 -> 상한 10
        assertEquals(10, result.interactionScore());
        assertEquals(8, result.dayScore());
    }

    /** 저녁 피크 x 평일에서 순수하게 DayWeight로만 생기는 interaction 점수. */
    private int peakInteraction(int marketDayWeight) {
        return peakInteractionOn(DayDemandLevel.WEEKDAY, marketDayWeight);
    }

    private int peakInteractionOn(DayDemandLevel dayDemandLevel, int marketDayWeight) {
        return calculator.calculate(
                TimeDemandLevel.VERY_HIGH,
                dayDemandLevel,
                marketDayWeight,
                new WeatherScoreResult(0, List.of(), "날씨 영향 없음"),
                noRainWeather(),
                0
        ).interactionScore();
    }

    /** 시간/날씨/대기질 영향이 없는 상태에서 요일 점수만 보기 위한 헬퍼. */
    private ScoreCalculationResult calmMediumDay(DayDemandLevel dayDemandLevel, int marketDayWeight) {
        return calculator.calculate(
                TimeDemandLevel.MEDIUM,
                dayDemandLevel,
                marketDayWeight,
                new WeatherScoreResult(0, List.of(), "날씨 영향 없음"),
                noRainWeather(),
                0
        );
    }

    private CurrentWeatherObservation noRainWeather() {
        return new CurrentWeatherObservation(0, 0, 20, 50, 1);
    }

    private CurrentWeatherObservation rainyWeather() {
        return new CurrentWeatherObservation(1, 5, 20, 80, 3);
    }
}
