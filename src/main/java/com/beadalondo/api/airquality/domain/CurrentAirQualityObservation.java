package com.beadalondo.api.airquality.domain;

import java.time.LocalDateTime;

public class CurrentAirQualityObservation {

    private final String sidoName;      // 시도명, 예: 서울
    private final String stationName;   // 측정소명, 예: 성동구
    private final String stationCode;   // 측정소 코드
    private final String mangName;      // 측정망, 예: 도시대기

    private final LocalDateTime measuredAt; // 실제 측정 시각

    private final Integer pm10Value;    // PM10 미세먼지 농도
    private final Integer pm25Value;    // PM2.5 초미세먼지 농도
    private final Double o3Value;       // 오존 농도

    private final Integer khaiValue;    // 통합대기환경수치
    private final Integer khaiGrade;    // 통합대기환경등급

    private final Integer pm10Grade;    // PM10 등급
    private final Integer pm25Grade;    // PM2.5 등급
    private final Integer pm10Grade1h;  // PM10 1시간 등급
    private final Integer pm25Grade1h;  // PM2.5 1시간 등급
    private final Integer o3Grade;      // O3 등급

    public CurrentAirQualityObservation(
            String sidoName,
            String stationName,
            String stationCode,
            String mangName,
            LocalDateTime measuredAt,
            Integer pm10Value,
            Integer pm25Value,
            Double o3Value,
            Integer khaiValue,
            Integer khaiGrade,
            Integer pm10Grade,
            Integer pm25Grade,
            Integer pm10Grade1h,
            Integer pm25Grade1h,
            Integer o3Grade
    ){
        this.sidoName = sidoName;
        this.stationName = stationName;
        this.stationCode = stationCode;
        this.mangName = mangName;
        this.measuredAt = measuredAt;
        this.pm10Value = pm10Value;
        this.pm25Value = pm25Value;
        this.o3Value = o3Value;
        this.khaiValue = khaiValue;
        this.khaiGrade = khaiGrade;
        this.pm10Grade = pm10Grade;
        this.pm25Grade = pm25Grade;
        this.pm10Grade1h = pm10Grade1h;
        this.pm25Grade1h = pm25Grade1h;
        this.o3Grade = o3Grade;
    }

    public String getSidoName() {
        return sidoName;
    }

    public String getStationName() {
        return stationName;
    }

    public String getStationCode() {
        return stationCode;
    }

    public String getMangName() {
        return mangName;
    }

    public LocalDateTime getMeasuredAt() {
        return measuredAt;
    }

    public Integer getPm10Value() {
        return pm10Value;
    }

    public Integer getPm25Value() {
        return pm25Value;
    }

    public Double getO3Value() {
        return o3Value;
    }

    public Integer getKhaiValue() {
        return khaiValue;
    }

    public Integer getKhaiGrade() {
        return khaiGrade;
    }

    public Integer getPm10Grade() {
        return pm10Grade;
    }

    public Integer getPm25Grade() {
        return pm25Grade;
    }

    public Integer getPm10Grade1h() {
        return pm10Grade1h;
    }

    public Integer getPm25Grade1h() {
        return pm25Grade1h;
    }

    public Integer getO3Grade() {
        return o3Grade;
    }
}
