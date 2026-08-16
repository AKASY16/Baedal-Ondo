package com.baedalondo.api.weather.domain;

import java.time.LocalDateTime;

public class ForecastWeatherObservation implements WeatherMeasurement {

    private final LocalDateTime forecastAt;
    private final int precipitationType; // PTY, 강수형태
    private final double rainfall;       // RN1, 1시간 강수량
    private final double temperature;    // T1H, 기온
    private final int humidity;          // REH, 습도
    private final double windSpeed;      // WSD, 풍속

    public ForecastWeatherObservation(LocalDateTime forecastAt,
                                      int precipitationType,
                                     double rainfall,
                                     double temperature,
                                     int humidity,
                                     double windSpeed) {
        this.forecastAt = forecastAt;
        this.precipitationType = precipitationType;
        this.rainfall = rainfall;
        this.temperature = temperature;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
    }

    public LocalDateTime getForecastAt() {
        return forecastAt;
    }

    public int getPrecipitationType() {
        return precipitationType;
    }

    public double getRainfall() {
        return rainfall;
    }

    public double getTemperature() {
        return temperature;
    }

    public int getHumidity() {
        return humidity;
    }

    public double getWindSpeed() {
        return windSpeed;
    }

}
