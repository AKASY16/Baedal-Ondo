package com.baedalondo.api.weather.service;

import com.baedalondo.api.score.calculator.KmaTimeCalculator;
import com.baedalondo.api.score.dto.ScoreTarget;
import com.baedalondo.api.weather.domain.CurrentWeatherObservation;
import com.baedalondo.api.weather.client.KmaCurrentWeatherClient;
import com.baedalondo.api.weather.domain.CurrentWeatherRecord;
import com.baedalondo.api.weather.repository.CurrentWeatherRecordRepository;
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

    /**
     * 날씨 캐시는 Store ID가 아니라 nx/ny/baseDate/baseTime 조합으로 재사용한다.
     * 매장이 달라도 같은 격자와 같은 기상청 기준 시간이면 DB 캐시를 사용한다.
     *
     * 기준 시간이 바뀌었거나 아직 저장되지 않은 격자라면 그 조합의 첫 요청에서
     * 기상청 API 응답 시간이 사용자 대시보드 지연으로 그대로 드러난다.
     *
     * 추후 개선 방향:
     * 스케줄러가 기준 시간이 바뀐 뒤 등록된 매장들의 고유 nx/ny 조합을 순회하며
     * current_weather_record를 사전 적재하면, 사용자 요청은 대부분 캐시 경로를 탄다.
     */
    public CurrentWeatherObservation getCurrentWeather(ScoreTarget scoreTarget) {
        if (scoreTarget == null) {
            throw new IllegalArgumentException("가게 정보가 없습니다.");
        }

        if (scoreTarget.getNx() == null || scoreTarget.getNy() == null) {
            throw new IllegalStateException("가게의 기상청 격자 좌표가 없습니다.");
        }

        int nx = scoreTarget.getNx();
        int ny = scoreTarget.getNy();

        LocalDateTime baseDateTime = kmaTimeCalculator.getSafeBaseDateTime();
        String baseDate = baseDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseTime = baseDateTime.format(DateTimeFormatter.ofPattern("HH00"));

        Optional<CurrentWeatherRecord> savedWeather =
                currentWeatherRecordRepository.findByNxAndNyAndBaseDateAndBaseTime(nx, ny, baseDate, baseTime);

        if (savedWeather.isPresent()) {
            return savedWeather.get().toObservation();
        }

        CurrentWeatherObservation weather = kmaCurrentWeatherClient.getCurrentWeather(
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

        currentWeatherRecordRepository.save(record);

        return weather;
    }
}
