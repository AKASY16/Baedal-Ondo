package com.beadalondo.api.weather.service;

import com.beadalondo.api.score.calculator.KmaTimeCalculator;
import com.beadalondo.api.store.domain.Store;
import com.beadalondo.api.weather.domain.CurrentWeatherObservation;
import com.beadalondo.api.weather.client.KmaCurrentWeatherClient;
import com.beadalondo.api.weather.domain.CurrentWeatherRecord;
import com.beadalondo.api.weather.repository.CurrentWeatherRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class CurrentWeatherService {

    private final KmaCurrentWeatherClient kmaCurrentWeatherClient;
    private final KmaTimeCalculator kmaTimeCalculator;
    private final CurrentWeatherRecordRepository currentWeatherRecordRepository;

    public CurrentWeatherService(KmaCurrentWeatherClient kmaCurrentWeatherClient,
                                 KmaTimeCalculator kmaTimeCalculator,
                                 CurrentWeatherRecordRepository currentWeatherRecordRepository) {
        this.kmaCurrentWeatherClient = kmaCurrentWeatherClient;
        this.kmaTimeCalculator = kmaTimeCalculator;
        this.currentWeatherRecordRepository = currentWeatherRecordRepository;
    }

    public CurrentWeatherObservation getCurrentWeather(Store store) {
        if (store == null) {
            throw new IllegalArgumentException("가게 정보가 없습니다.");
        }

        if (store.getNx() == null || store.getNy() == null) {
            throw new IllegalStateException("가게의 기상청 격자 좌표가 없습니다.");
        }

        int nx = store.getNx();
        int ny = store.getNy();

        LocalDateTime baseDateTime = kmaTimeCalculator.getSafeBaseDateTime();

        String baseDate = baseDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseTime = baseDateTime.format(DateTimeFormatter.ofPattern("HH00"));

        Optional<CurrentWeatherRecord> savedWeather =
                currentWeatherRecordRepository.findByNxAndNyAndBaseDateAndBaseTime(nx, ny, baseDate, baseTime);

        if (savedWeather.isPresent()) {
            log.info("저장된 날씨 데이터 재사용: nx={}, ny={}, baseDate={}, baseTime={}",
                    nx, ny, baseDate, baseTime);
            return savedWeather.get().toObservation();
        }

        CurrentWeatherObservation weather =
                kmaCurrentWeatherClient.getCurrentWeather(
                        nx,
                        ny,
                        baseDate,
                        baseTime
                );

        CurrentWeatherRecord record = CurrentWeatherRecord.from(
                nx,
                ny,
                baseDate,
                baseTime,
                weather
        );

        log.info("저장된 날씨 데이터가 없어 기상청 API 호출: nx={}, ny={}, baseDate={}, baseTime={}",
                nx, ny, baseDate, baseTime);

        currentWeatherRecordRepository.save(record);

        log.info("현재 날씨 데이터 저장 완료: nx={}, ny={}, baseDate={}, baseTime={}",
                nx, ny, baseDate, baseTime);

        return weather;
    }

    private static final Logger log = LoggerFactory.getLogger(CurrentWeatherService.class);
}