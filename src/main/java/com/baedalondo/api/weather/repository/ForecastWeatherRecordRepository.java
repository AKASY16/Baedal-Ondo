package com.baedalondo.api.weather.repository;

import com.baedalondo.api.weather.domain.ForecastWeatherRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ForecastWeatherRecordRepository extends JpaRepository<ForecastWeatherRecord, Long> {

    /**
     * 같은 발표분의 예보 여러 건을 예보 시각 순으로 가져온다.
     * 화면이 1시간 후부터 6시간 후까지 순서대로 보여주므로 정렬이 필요하다.
     */
    List<ForecastWeatherRecord> findByNxAndNyAndBaseDateAndBaseTimeOrderByForecastAtAsc(
            int nx,
            int ny,
            String baseDate,
            String baseTime
    );

}
