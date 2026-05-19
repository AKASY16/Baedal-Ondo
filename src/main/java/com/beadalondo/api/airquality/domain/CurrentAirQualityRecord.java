package com.beadalondo.api.airquality.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "current_air_quality_record",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_air_quality_record_location_station_time",
                        columnNames = {"sido_name",
                                "district_name",
                                "station_name",
                                "measured_at"}
                )
        }
)
public class CurrentAirQualityRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 공기질 기록 고유 ID

    private String sidoName; // 시도명, 예: 서울, 경기, 부산
    private String districtName; // 시군구명, 예: 성동구, 강남구

    private String stationName; // 실제 데이터를 가져온 측정소명, 예: 성동구
    private String stationCode; // 에어코리아 측정소 코드, 예: 111142

    private String mangName; // 측정망 종류, 예: 도시대기, 도로변대기

    private LocalDateTime measuredAt; // 에어코리아 측정 시각, dataTime 값

    private Integer pm10Value; // 미세먼지 PM10 농도, 단위: ㎍/㎥
    private Integer pm25Value; // 초미세먼지 PM2.5 농도, 단위: ㎍/㎥
    private Double o3Value; // 오존 O3 농도, 단위: ppm

    private Integer khaiValue; // 통합대기환경수치
    private Integer khaiGrade; // 통합대기환경등급, 1 좋음 / 2 보통 / 3 나쁨 / 4 매우나쁨

    private Integer pm10Grade; // PM10 24시간 등급, 1 좋음 / 2 보통 / 3 나쁨 / 4 매우나쁨
    private Integer pm25Grade; // PM2.5 24시간 등급, 1 좋음 / 2 보통 / 3 나쁨 / 4 매우나쁨

    @Column(name = "pm10_grade_1h")
    private Integer pm10Grade1h;// PM10 1시간 등급, 현재 시간대 미세먼지 상태
    @Column(name = "pm25_grade_1h")
    private Integer pm25Grade1h;// PM2.5 1시간 등급, 현재 시간대 초미세먼지 상태

    private Integer o3Grade; // 오존 O3 등급, 1 좋음 / 2 보통 / 3 나쁨 / 4 매우나쁨

    private LocalDateTime createdAt; // DB에 이 기록이 저장된 시각




    public Long getId() {
        return id;
    }

    public String getSidoName() {
        return sidoName;
    }

    public String getDistrictName() {
        return districtName;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    //주석 처리한 setter들 접어놓은 곳
//
//
//    public void setId(Long id) {
//        this.id = id;
//    }
//
//    public void setSidoName(String sidoName) {
//        this.sidoName = sidoName;
//    }
//
//    public void setDistrictName(String districtName) {
//        this.districtName = districtName;
//    }
//
//    public void setStationName(String stationName) {
//        this.stationName = stationName;
//    }
//
//    public void setStationCode(String stationCode) {
//        this.stationCode = stationCode;
//    }
//
//    public void setMangName(String mangName) {
//        this.mangName = mangName;
//    }
//
//    public void setMeasuredAt(LocalDateTime measuredAt) {
//        this.measuredAt = measuredAt;
//    }
//
//    public void setPm10Value(Integer pm10Value) {
//        this.pm10Value = pm10Value;
//    }
//
//    public void setPm25Value(Integer pm25Value) {
//        this.pm25Value = pm25Value;
//    }
//
//    public void setO3Value(Double o3Value) {
//        this.o3Value = o3Value;
//    }
//
//    public void setKhaiValue(Integer khaiValue) {
//        this.khaiValue = khaiValue;
//    }
//
//    public void setKhaiGrade(Integer khaiGrade) {
//        this.khaiGrade = khaiGrade;
//    }
//
//    public void setPm10Grade(Integer pm10Grade) {
//        this.pm10Grade = pm10Grade;
//    }
//
//    public void setPm25Grade(Integer pm25Grade) {
//        this.pm25Grade = pm25Grade;
//    }
//
//    public void setPm10Grade1h(Integer pm10Grade1h) {
//        this.pm10Grade1h = pm10Grade1h;
//    }
//
//    public void setPm25Grade1h(Integer pm25Grade1h) {
//        this.pm25Grade1h = pm25Grade1h;
//    }
//
//    public void setO3Grade(Integer o3Grade) {
//        this.o3Grade = o3Grade;
//    }
//
//    public void setCreatedAt(LocalDateTime createdAt) {
//        this.createdAt = createdAt;
//    }

    public CurrentAirQualityObservation toObservation() {
        return new CurrentAirQualityObservation(
                sidoName,
                stationName,
                stationCode,
                mangName,
                measuredAt,
                pm10Value,
                pm25Value,
                o3Value,
                khaiValue,
                khaiGrade,
                pm10Grade,
                pm25Grade,
                pm10Grade1h,
                pm25Grade1h,
                o3Grade
        );
    }

    public static CurrentAirQualityRecord from(
            String sidoName,
            String districtName,
            CurrentAirQualityObservation observation
    ) {
        CurrentAirQualityRecord record = new CurrentAirQualityRecord();

        record.sidoName = sidoName;
        record.districtName = districtName;

        record.stationName = observation.getStationName();
        record.stationCode = observation.getStationCode();

        record.mangName = observation.getMangName();

        record.measuredAt = observation.getMeasuredAt();

        record.pm10Value = observation.getPm10Value();
        record.pm25Value = observation.getPm25Value();
        record.o3Value = observation.getO3Value();

        record.khaiValue = observation.getKhaiValue();
        record.khaiGrade = observation.getKhaiGrade();

        record.pm10Grade = observation.getPm10Grade();
        record.pm25Grade = observation.getPm25Grade();
        record.pm10Grade1h = observation.getPm10Grade1h();
        record.pm25Grade1h = observation.getPm25Grade1h();
        record.o3Grade = observation.getO3Grade();

        record.createdAt = LocalDateTime.now();

        return record;
    }


}
