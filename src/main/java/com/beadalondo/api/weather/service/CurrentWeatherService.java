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
        long totalStart = System.nanoTime();
        Long storeId = storeId(store);

        try {
        if (store == null) {
            throw new IllegalArgumentException("가게 정보가 없습니다.");
        }

        if (store.getNx() == null || store.getNy() == null) {
            throw new IllegalStateException("가게의 기상청 격자 좌표가 없습니다.");
        }

        int nx = store.getNx();
        int ny = store.getNy();

        LocalDateTime baseDateTime;
        long baseTimeStart = System.nanoTime();
        try {
            baseDateTime = kmaTimeCalculator.getSafeBaseDateTime();
        } finally {
            logTiming("weatherBaseTime", baseTimeStart, storeId);
        }

        String baseDate = baseDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseTime = baseDateTime.format(DateTimeFormatter.ofPattern("HH00"));

        // Troubleshooting note:
        // 대시보드는 현재 Store를 랜덤으로 선택하지만, 날씨 캐시는 Store ID가 아니라
        // nx/ny/baseDate/baseTime 조합으로 재사용한다. Store가 바뀌어도 같은 격자와
        // 같은 기상청 기준 시간이면 DB 캐시를 사용한다.
        //
        // 단, 기준 시간이 바뀌었거나 아직 저장되지 않은 격자가 선택되면 해당 조합의 최초
        // 요청에서 기상청 API 호출 시간이 사용자 대시보드 응답 지연으로 그대로 드러난다.
        // 이 지연은 우리 계산/DB 병목이 아니라 외부 API 응답 시간에 좌우된다.
        //
        // 추후 개선 방향:
        // 스케줄러가 기상청 기준 시간이 바뀐 뒤 등록된 Store들의 고유 nx/ny 조합을 순회하며
        // current_weather_record를 사전 적재한다. 그러면 사용자 대시보드 요청은 대부분
        // 캐시 HIT 경로를 타고, 최초 외부 API 호출 지연이 사용자 요청에 노출되지 않는다.
            // 현재는 최대 시간 27초까지도 보이지만, 더 줄일 수 있도록 하기.
        Optional<CurrentWeatherRecord> savedWeather;
        long dbLookupStart = System.nanoTime();
        try {
            savedWeather = currentWeatherRecordRepository.findByNxAndNyAndBaseDateAndBaseTime(nx, ny, baseDate, baseTime);
        } finally {
            logTiming("weatherDbLookup", dbLookupStart, storeId);
        }

        if (savedWeather.isPresent()) {
            log.info("저장된 날씨 데이터 재사용: nx={}, ny={}, baseDate={}, baseTime={}",
                    nx, ny, baseDate, baseTime);
            return savedWeather.get().toObservation();
        }

        CurrentWeatherObservation weather;
        long apiStart = System.nanoTime();
        try {
            weather = kmaCurrentWeatherClient.getCurrentWeather(
                    nx,
                    ny,
                    baseDate,
                    baseTime
            );
        } finally {
            logTiming("weatherApi", apiStart, storeId);
        }

        CurrentWeatherRecord record = CurrentWeatherRecord.from(
                nx,
                ny,
                baseDate,
                baseTime,
                weather
        );

        log.info("저장된 날씨 데이터가 없어 기상청 API 호출: nx={}, ny={}, baseDate={}, baseTime={}",
                nx, ny, baseDate, baseTime);

        long dbSaveStart = System.nanoTime();
        try {
            currentWeatherRecordRepository.save(record);
        } finally {
            logTiming("weatherDbSave", dbSaveStart, storeId);
        }

        log.info("현재 날씨 데이터 저장 완료: nx={}, ny={}, baseDate={}, baseTime={}",
                nx, ny, baseDate, baseTime);

        return weather;
        } finally {
            logTiming("weatherTotal", totalStart, storeId);
        }
    }

    private void logTiming(String step, long startNanos, Long storeId) {
        log.info("dashboard timing step={} elapsedMs={} storeId={}",
                step,
                elapsedMs(startNanos),
                storeId);
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private Long storeId(Store store) {
        return store == null ? null : store.getId();
    }

    private static final Logger log = LoggerFactory.getLogger(CurrentWeatherService.class);
}
