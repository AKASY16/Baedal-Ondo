package com.beadalondo.api.weather.repository;

import com.beadalondo.api.weather.domain.CurrentWeatherRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CurrentWeatherRecordRepository
        extends JpaRepository<CurrentWeatherRecord, Long> {



    Optional<CurrentWeatherRecord> findByNxAndNyAndBaseDateAndBaseTime(
            int nx,
            int ny,
            String baseDate,
            String baseTime
    );
}
