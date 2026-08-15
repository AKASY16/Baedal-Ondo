package com.baedalondo.api.airquality.domain;

import com.baedalondo.api.common.ServiceTime;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 특정 시도의 특정 기준시각 데이터를 이미 조회했는지 기록한다.
 *
 * 측정값의 measuredAt과 우리 서버의 조회 주기는 다른 개념이다.
 * measuredAt은 측정소가 실제로 측정한 시각이고, 여기의 baseTime은
 * AirQualityCalculator가 계산한 우리 기준시각이다.
 *
 * 측정값만 보고 재사용을 판단하면 "아직 안 받아왔다"와 "받아왔는데 그 자치구
 * 측정소가 응답에 없었다"를 구분할 수 없어 매번 API를 다시 호출하게 된다.
 */
@Entity
@Table(
        name = "air_quality_fetch_log",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_air_quality_fetch_log_sido_base_time",
                        columnNames = {"sido_name", "base_time"}
                )
        }
)
public class AirQualityFetchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sidoName; // 시도명, 예: 서울

    @Column(nullable = false)
    private LocalDateTime baseTime; // 우리 서버가 사용한 기준시각

    @Column(nullable = false)
    private LocalDateTime fetchedAt; // 해당 기준시각 데이터를 확인한 시각

    protected AirQualityFetchLog() {
    }

    private AirQualityFetchLog(String sidoName, LocalDateTime baseTime, LocalDateTime fetchedAt) {
        this.sidoName = sidoName;
        this.baseTime = baseTime;
        this.fetchedAt = fetchedAt;
    }

    public static AirQualityFetchLog of(String sidoName, LocalDateTime baseTime) {
        return new AirQualityFetchLog(sidoName, baseTime, ServiceTime.now());
    }

    public Long getId() {
        return id;
    }

    public String getSidoName() {
        return sidoName;
    }

    public LocalDateTime getBaseTime() {
        return baseTime;
    }

    public LocalDateTime getFetchedAt() {
        return fetchedAt;
    }
}
