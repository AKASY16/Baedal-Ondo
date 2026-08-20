package com.baedalondo.api.airquality.repository;

import com.baedalondo.api.airquality.domain.CurrentAirQualityRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CurrentAirQualityRecordRepository
        extends JpaRepository<CurrentAirQualityRecord, Long> {

    /**
     * 기준시각 이후에 측정된 것만 찾는다.
     *
     * 시간 조건 없이 제일 최근 값을 쓰면 몇 시간 전 측정값이 현재 값처럼 화면에 나간다.
     * 조회 기록도 기준시각으로 판단하므로 둘의 기준을 맞춘다.
     */
    Optional<CurrentAirQualityRecord>
    findTopBySidoNameAndDistrictNameAndMeasuredAtGreaterThanEqualOrderByMeasuredAtDescCreatedAtDesc(
            String sidoName,
            String districtName,
            LocalDateTime measuredAtFrom
    );

    boolean existsBySidoNameAndDistrictNameAndStationNameAndMeasuredAt(
            String sidoName,
            String districtName,
            String stationName,
            LocalDateTime measuredAt
    );

    Optional<CurrentAirQualityRecord> findBySidoNameAndDistrictNameAndStationNameAndMeasuredAt(
            String sidoName,
            String districtName,
            String stationName,
            LocalDateTime measuredAt
    );
}
