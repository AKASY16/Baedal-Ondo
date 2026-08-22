package com.baedalondo.api.weather.domain;

/**
 * 날씨 점수 계산에 필요한 값들.
 *
 * 지금 구현은 예보(ForecastWeatherObservation) 하나다. 점수 계산이 관측의 출처와
 * 시각 해석에 묶이지 않도록 이 자리를 남겨 둔다. 기상청 초단기실황과 초단기예보가
 * 같은 카테고리(PTY, RN1, T1H, WSD)를 주므로, 과거 관측을 다시 들이더라도
 * 계산 로직은 한 벌로 유지된다.
 */
public interface WeatherMeasurement {

    int getPrecipitationType(); // PTY, 강수형태

    double getRainfall();       // RN1, 1시간 강수량

    double getTemperature();    // T1H, 기온

    double getWindSpeed();      // WSD, 풍속
}
