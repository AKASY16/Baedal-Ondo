package com.baedalondo.api.weather.domain;

/**
 * 날씨 점수 계산에 필요한 값들.
 *
 * 실황(CurrentWeatherObservation)과 예보(ForecastWeatherObservation)는
 * 시각을 가리키는 의미가 다를 뿐, 점수 계산에 쓰는 항목은 같다.
 * 기상청 초단기실황과 초단기예보가 같은 카테고리(PTY, RN1, T1H, WSD)를 주기 때문이다.
 *
 * 이 인터페이스를 두어 점수 계산 로직을 한 벌만 유지한다.
 */
public interface WeatherMeasurement {

    int getPrecipitationType(); // PTY, 강수형태

    double getRainfall();       // RN1, 1시간 강수량

    double getTemperature();    // T1H, 기온

    double getWindSpeed();      // WSD, 풍속
}
