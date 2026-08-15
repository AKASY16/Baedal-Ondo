package com.baedalondo.api.weather.domain;


import com.baedalondo.api.common.ServiceTime;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 초단기예보 1건을 담는다.
 *
 * 한 번의 API 호출이 +1시간부터 +6시간까지 여러 예보 시각을 돌려주므로,
 * 같은 base_time에 대해 이 레코드가 여러 행 저장된다.
 */
@Entity
@Table(
        name = "forecast_weather_record",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_forecast_weather_record_location_time",
                        // columnNames는 자바 필드명이 아니라 DB 컬럼명이다.
                        columnNames = {"forecast_at", "nx", "ny", "base_date", "base_time"}
                )
        }
)
public class ForecastWeatherRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime forecastAt; // 기상 시각

    private int nx; // 기상청 API NX값
    private int ny; // 기상청 API NY값
    private String baseDate; // 요청 날짜
    private String baseTime; // 요청 시각

    private int precipitationType; // 강수량
    private double rainfall; // 비 여부
    private double temperature; // 온도
    private int humidity; // 습도
    private double windSpeed; // 풍속

    private LocalDateTime createdAt; // 데이터 생성 시각

    public Long getId() {
        return id;
    }

    public LocalDateTime getForecastAt() {
        return forecastAt;
    }

    public int getNx() {
        return nx;
    }

    public int getNy() {
        return ny;
    }

    public String getBaseDate() {
        return baseDate;
    }

    public String getBaseTime() {
        return baseTime;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public ForecastWeatherObservation toObservation() {
        return new ForecastWeatherObservation(
                forecastAt,
                precipitationType,
                rainfall,
                temperature,
                humidity,
                windSpeed
        );
    }

    /**
     * 예보 한 건을 레코드로 만든다.
     * 여러 예보 시각은 호출하는 쪽에서 반복해 저장한다.
     */
    public static ForecastWeatherRecord from(
            int nx,
            int ny,
            String baseDate,
            String baseTime,
            ForecastWeatherObservation weather
    ) {
        ForecastWeatherRecord record = new ForecastWeatherRecord();

        record.forecastAt = weather.getForecastAt();

        record.nx = nx;
        record.ny = ny;
        record.baseDate = baseDate;
        record.baseTime = baseTime;

        record.precipitationType = weather.getPrecipitationType();
        record.rainfall = weather.getRainfall();
        record.temperature = weather.getTemperature();
        record.humidity = weather.getHumidity();
        record.windSpeed = weather.getWindSpeed();

        record.createdAt = ServiceTime.now();

        return record;
    }
}
