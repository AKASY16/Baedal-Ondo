package com.baedalondo.api.weather.service;

import com.baedalondo.api.score.calculator.KmaTimeCalculator;
import com.baedalondo.api.score.dto.ScoreTarget;
import com.baedalondo.api.weather.client.KmaForecastWeatherClient;
import com.baedalondo.api.weather.domain.ForecastWeatherObservation;
import com.baedalondo.api.weather.domain.ForecastWeatherRecord;
import com.baedalondo.api.weather.repository.ForecastWeatherRecordRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
public class ForecastWeatherService {

    private static final DateTimeFormatter BASE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter BASE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmm");

    private final KmaForecastWeatherClient kmaForecastWeatherClient;
    private final KmaTimeCalculator kmaTimeCalculator;
    private final ForecastWeatherRecordRepository forecastWeatherRecordRepository;

    public ForecastWeatherService(
            KmaForecastWeatherClient kmaForecastWeatherClient,
            KmaTimeCalculator kmaTimeCalculator,
            ForecastWeatherRecordRepository forecastWeatherRecordRepository
    ) {
        this.kmaForecastWeatherClient = kmaForecastWeatherClient;
        this.kmaTimeCalculator = kmaTimeCalculator;
        this.forecastWeatherRecordRepository = forecastWeatherRecordRepository;
    }

    public List<ForecastWeatherObservation> getForecastWeather(ScoreTarget scoreTarget) {
        if (scoreTarget == null) {
            throw new IllegalArgumentException("가게 정보가 없습니다.");
        }

        if (scoreTarget.getNx() == null || scoreTarget.getNy() == null) {
            throw new IllegalStateException("가게의 기상청 격자 좌표가 없습니다.");
        }

        int nx = scoreTarget.getNx();
        int ny = scoreTarget.getNy();

        // 초단기예보는 실황과 발표 주기가 달라 예보 전용 기준 시각을 쓴다.
        LocalDateTime baseDateTime = kmaTimeCalculator.getSafeForecastBaseDateTime();
        String baseDate = baseDateTime.format(BASE_DATE_FORMATTER);
        String baseTime = baseDateTime.format(BASE_TIME_FORMATTER);

        List<ForecastWeatherRecord> savedForecastWeather =
                forecastWeatherRecordRepository.findByNxAndNyAndBaseDateAndBaseTimeOrderByForecastAtAsc(
                        nx, ny, baseDate, baseTime);

        if (!savedForecastWeather.isEmpty()) {
            return savedForecastWeather.stream()
                    .map(ForecastWeatherRecord::toObservation)
                    .toList();
        }

        List<ForecastWeatherObservation> forecastWeather = kmaForecastWeatherClient.getForecastWeather(
                nx,
                ny,
                baseDate,
                baseTime
        );

        // 예보 1건이 레코드 1행이다. 한 번의 호출에서 받은 여러 시각을 모두 저장한다.
        List<ForecastWeatherRecord> records = forecastWeather.stream()
                .map(observation -> ForecastWeatherRecord.from(nx, ny, baseDate, baseTime, observation))
                .toList();

        forecastWeatherRecordRepository.saveAll(records);

        return forecastWeather.stream()
                .sorted(Comparator.comparing(ForecastWeatherObservation::getForecastAt))
                .toList();
    }
}
