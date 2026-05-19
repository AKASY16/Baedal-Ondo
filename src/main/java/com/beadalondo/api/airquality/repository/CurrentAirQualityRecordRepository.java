package com.beadalondo.api.airquality.repository;

import com.beadalondo.api.airquality.domain.CurrentAirQualityRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CurrentAirQualityRecordRepository
        extends JpaRepository<CurrentAirQualityRecord, Long> {

    Optional<CurrentAirQualityRecord> findTopBySidoNameAndDistrictNameOrderByMeasuredAtDescCreatedAtDesc(
            String sidoName,
            String districtName
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
