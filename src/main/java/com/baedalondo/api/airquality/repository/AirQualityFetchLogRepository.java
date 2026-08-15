package com.baedalondo.api.airquality.repository;

import com.baedalondo.api.airquality.domain.AirQualityFetchLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface AirQualityFetchLogRepository extends JpaRepository<AirQualityFetchLog, Long> {

    boolean existsBySidoNameAndBaseTime(String sidoName, LocalDateTime baseTime);
}
