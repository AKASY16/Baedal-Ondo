package com.baedalondo.api.weather.calculator;

import com.baedalondo.api.weather.domain.CurrentWeatherObservation;
import com.baedalondo.api.weather.domain.ForecastWeatherObservation;
import com.baedalondo.api.weather.domain.WeatherScoreResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForecastWeatherWeightCalculatorTest {

    private final WeatherWeightCalculator weatherWeightCalculator = new WeatherWeightCalculator();
    private final ForecastWeatherWeightCalculator forecastCalculator =
            new ForecastWeatherWeightCalculator(weatherWeightCalculator);
    private final CurrentWeatherWeightCalculator currentCalculator =
            new CurrentWeatherWeightCalculator(weatherWeightCalculator);

    @Test
    @DisplayName("같은 관측값이면 실황과 예보의 점수가 동일하다")
    void producesSameScoreAsCurrentWeatherForSameValues() {
        ForecastWeatherObservation forecast = createForecast(
                LocalDateTime.of(2026, 8, 16, 20, 0), 1, 5.0, 3.0, 12.0);
        CurrentWeatherObservation current = new CurrentWeatherObservation(1, 5.0, 3.0, 60, 12.0);

        WeatherScoreResult forecastResult = forecastCalculator.calculate(forecast);
        WeatherScoreResult currentResult = currentCalculator.calculate(current);

        assertEquals(currentResult.getWeatherScore(), forecastResult.getWeatherScore());
        assertEquals(currentResult.getDescription(), forecastResult.getDescription());
    }

    @Test
    @DisplayName("비가 오지 않고 기온이 온화하면 날씨 영향이 없다")
    void returnsNoImpactForCalmWeather() {
        ForecastWeatherObservation forecast = createForecast(
                LocalDateTime.of(2026, 8, 16, 20, 0), 0, 0.0, 20.0, 1.0);

        WeatherScoreResult result = forecastCalculator.calculate(forecast);

        assertEquals(0, result.getWeatherScore());
        assertEquals("외출에 큰 불편이 없는 날씨", result.getDescription());
    }

    @Test
    @DisplayName("강수량이 많으면 강수량이 요인으로 잡힌다")
    void reflectsHeavyRainfall() {
        // PTY 1(비)은 강수형태 점수가 0이다. 비의 영향은 강수량으로 반영한다.
        ForecastWeatherObservation forecast = createForecast(
                LocalDateTime.of(2026, 8, 16, 20, 0), 1, 20.0, 18.0, 1.0);

        WeatherScoreResult result = forecastCalculator.calculate(forecast);

        assertTrue(result.getWeatherScore() > 0);
        assertTrue(result.getFactors().contains("강수량"));
    }

    @Test
    @DisplayName("눈은 강수형태로도 점수가 붙는다")
    void reflectsSnowAsPrecipitationType() {
        ForecastWeatherObservation forecast = createForecast(
                LocalDateTime.of(2026, 8, 16, 20, 0), 3, 2.0, -3.0, 1.0);

        WeatherScoreResult result = forecastCalculator.calculate(forecast);

        assertTrue(result.getFactors().contains("강수형태"));
        assertTrue(result.getFactors().contains("강수량"));
    }

    @Test
    @DisplayName("여러 예보 시각을 입력 순서대로 계산한다")
    void calculatesAllForecastsInOrder() {
        LocalDateTime first = LocalDateTime.of(2026, 8, 16, 20, 0);
        LocalDateTime second = LocalDateTime.of(2026, 8, 16, 21, 0);
        LocalDateTime third = LocalDateTime.of(2026, 8, 16, 22, 0);

        Map<LocalDateTime, WeatherScoreResult> results = forecastCalculator.calculateAll(List.of(
                createForecast(first, 0, 0.0, 20.0, 1.0),
                createForecast(second, 1, 5.0, 18.0, 1.0),
                createForecast(third, 0, 0.0, 20.0, 1.0)
        ));

        assertEquals(List.of(first, second, third), List.copyOf(results.keySet()));
        assertEquals(0, results.get(first).getWeatherScore());
        assertTrue(results.get(second).getWeatherScore() > 0);
    }

    @Test
    @DisplayName("예보가 null이면 거부한다")
    void rejectsNullForecast() {
        assertThrows(IllegalArgumentException.class, () -> forecastCalculator.calculate(null));
        assertThrows(IllegalArgumentException.class, () -> forecastCalculator.calculateAll(null));
    }

    private ForecastWeatherObservation createForecast(LocalDateTime forecastAt,
                                                      int precipitationType,
                                                      double rainfall,
                                                      double temperature,
                                                      double windSpeed) {
        return new ForecastWeatherObservation(
                forecastAt,
                precipitationType,
                rainfall,
                temperature,
                60,
                windSpeed
        );
    }
}
