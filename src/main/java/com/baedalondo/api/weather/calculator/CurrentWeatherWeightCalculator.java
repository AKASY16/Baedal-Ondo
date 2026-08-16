package com.baedalondo.api.weather.calculator;

import com.baedalondo.api.weather.domain.CurrentWeatherObservation;
import com.baedalondo.api.weather.domain.WeatherScoreResult;
import org.springframework.stereotype.Component;

/**
 * 현재 날씨 관측값의 점수를 낸다.
 *
 * 계산 규칙 자체는 예보와 같으므로 WeatherWeightCalculator에 두고,
 * 여기서는 실황 타입을 받는 진입점 역할만 한다.
 */
@Component
public class CurrentWeatherWeightCalculator {

    private final WeatherWeightCalculator weatherWeightCalculator;

    public CurrentWeatherWeightCalculator(WeatherWeightCalculator weatherWeightCalculator) {
        this.weatherWeightCalculator = weatherWeightCalculator;
    }

    public WeatherScoreResult calculate(CurrentWeatherObservation weather) {
        return weatherWeightCalculator.calculate(weather);
    }
}
