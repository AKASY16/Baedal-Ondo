package com.beadalondo.api.weather.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public class CurrentWeatherRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 고유 ID

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

    public CurrentWeatherObservation toObservation() {
        return new CurrentWeatherObservation(
                precipitationType,
                rainfall,
                temperature,
                humidity,
                windSpeed
        );
    }

    public static CurrentWeatherRecord from(
            int nx,
            int ny,
            String baseDate,
            String baseTime,
            CurrentWeatherObservation weather
    ) {
        CurrentWeatherRecord record = new CurrentWeatherRecord();

        record.nx = nx;
        record.ny = ny;
        record.baseDate = baseDate;
        record.baseTime = baseTime;

        record.precipitationType = weather.getPrecipitationType();
        record.rainfall = weather.getRainfall();
        record.temperature = weather.getTemperature();
        record.humidity = weather.getHumidity();
        record.windSpeed = weather.getWindSpeed();

        record.createdAt = LocalDateTime.now();

        return record;
    }

}
