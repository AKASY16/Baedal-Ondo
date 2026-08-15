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
                        // 조회 조건을 앞에, 정렬 기준인 forecast_at을 마지막에 둔다.
                        columnNames = {"nx", "ny", "base_date", "base_time", "forecast_at"}
                )
        }
)
public class ForecastWeatherRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime forecastAt; // 이 예보가 가리키는 시각

    private int nx; // 기상청 격자 X좌표
    private int ny; // 기상청 격자 Y좌표

    @Column(nullable = false)
    private String baseDate; // 예보 발표 날짜

    @Column(nullable = false)
    private String baseTime; // 예보 발표 시각

    private int precipitationType; // PTY, 강수형태
    private double rainfall;       // RN1, 1시간 강수량
    private double temperature;    // T1H, 기온
    private int humidity;          // REH, 습도
    private double windSpeed;      // WSD, 풍속

    @Column(nullable = false)
    private LocalDateTime createdAt; // 레코드 생성 시각

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
