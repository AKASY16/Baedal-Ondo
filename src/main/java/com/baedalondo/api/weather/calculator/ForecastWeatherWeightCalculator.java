package com.baedalondo.api.weather.calculator;

import com.baedalondo.api.weather.domain.ForecastWeatherObservation;
import com.baedalondo.api.weather.domain.WeatherScoreResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 예보 날씨의 점수를 낸다.
 *
 * 계산 규칙은 실황과 같으므로 WeatherWeightCalculator에 맡기고,
 * 여기서는 예보 타입을 받는 진입점과 여러 예보 시각을 한 번에 처리하는
 * 편의 메서드를 제공한다.
 *
 * 예보 문구를 실황과 다르게 가져가야 할 때 이 클래스가 그 자리가 된다.
 */
@Component
public class ForecastWeatherWeightCalculator {

    private final WeatherWeightCalculator weatherWeightCalculator;

    public ForecastWeatherWeightCalculator(WeatherWeightCalculator weatherWeightCalculator) {
        this.weatherWeightCalculator = weatherWeightCalculator;
    }

    public WeatherScoreResult calculate(ForecastWeatherObservation forecast) {
        if (forecast == null) {
            throw new IllegalArgumentException("예보 정보가 없습니다.");
        }

        return weatherWeightCalculator.calculate(forecast);
    }

    /**
     * 예보 시각별 점수를 한 번에 계산한다.
     * 1시간 후부터 6시간 후까지를 화면에 나열할 때 쓴다.
     * 입력 순서를 그대로 유지한다.
     */
    public Map<LocalDateTime, WeatherScoreResult> calculateAll(
            List<ForecastWeatherObservation> forecasts) {

        if (forecasts == null) {
            throw new IllegalArgumentException("예보 정보가 없습니다.");
        }

        Map<LocalDateTime, WeatherScoreResult> results = new LinkedHashMap<>();

        for (ForecastWeatherObservation forecast : forecasts) {
            results.put(forecast.getForecastAt(), calculate(forecast));
        }

        return results;
    }
}
