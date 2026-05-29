package com.beadalondo.api.score.service;

import com.beadalondo.api.airquality.calculator.AirQualityCalculator;
import com.beadalondo.api.airquality.domain.CurrentAirQualityObservation;
import com.beadalondo.api.airquality.exception.AirKoreaApiException;
import com.beadalondo.api.airquality.service.CurrentAirQualityService;
import com.beadalondo.api.holiday.service.HolidayService;
import com.beadalondo.api.score.ScoreResult;
import com.beadalondo.api.score.calculator.DayWeightCalculator;
import com.beadalondo.api.score.status.DayDemandLevel;
import com.beadalondo.api.score.status.TimeDemandLevel;
import com.beadalondo.api.score.calculator.TimeWeightCalculator;
import com.beadalondo.api.store.domain.Store;
import com.beadalondo.api.weather.calculator.CurrentWeatherWeightCalculator;
import com.beadalondo.api.weather.domain.CurrentWeatherObservation;
import com.beadalondo.api.weather.domain.WeatherScoreResult;
import com.beadalondo.api.weather.exception.KmaWeatherApiException;
import com.beadalondo.api.weather.service.CurrentWeatherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class ScoreService {

    private final TimeWeightCalculator timeWeightCalculator;
    private final DayWeightCalculator dayWeightCalculator;
    private final CurrentWeatherWeightCalculator currentWeatherWeightCalculator;
    private final CurrentWeatherService currentWeatherService;
    private final CurrentAirQualityService currentAirQualityService;
    private final AirQualityCalculator airQualityCalculator;
    private final HolidayService holidayService;

    public ScoreService(TimeWeightCalculator timeWeightCalculator,
                        DayWeightCalculator dayWeightCalculator,
                        CurrentWeatherWeightCalculator currentWeatherWeightCalculator,
                        CurrentWeatherService currentWeatherService,
                        CurrentAirQualityService currentAirQualityService,
                        AirQualityCalculator airQualityCalculator,
                        HolidayService holidayService) {
        this.timeWeightCalculator = timeWeightCalculator;
        this.dayWeightCalculator = dayWeightCalculator;
        this.currentWeatherWeightCalculator = currentWeatherWeightCalculator;
        this.currentWeatherService = currentWeatherService;
        this.currentAirQualityService = currentAirQualityService;
        this.airQualityCalculator = airQualityCalculator;
        this.holidayService = holidayService;
    }


    public ScoreResult calculateCurrentScore(Store store) {
        long totalStart = System.nanoTime();
        Long storeId = storeId(store);

        try {
            int baseScore = 40;
            int airQualityScore = 0;
            String airQualityFactor = "영향 없음";
            String airQualityDescription = "대기질";
            String airQualityDetail = "대기질 정보 없음";

            TimeDemandLevel timeDemandLevel;
            DayDemandLevel dayDemandLevel;
            long baseStart = System.nanoTime();
            try {
                LocalDate currentDate = LocalDate.now();
                timeDemandLevel = timeWeightCalculator.calculate(LocalTime.now());
                dayDemandLevel = dayWeightCalculator.calculate(currentDate, isHoliday(currentDate, storeId));
            } finally {
                logTiming("baseScore", baseStart, storeId);
            }

            WeatherScoreResult weatherScoreResult;
            long weatherStart = System.nanoTime();
            try {
                CurrentWeatherObservation weather = currentWeatherService.getCurrentWeather(store);
                weatherScoreResult = currentWeatherWeightCalculator.calculate(weather);
            } catch (KmaWeatherApiException e) {
                log.warn("기상청 API 에러. 날씨 보정 점수를 제외합니다. storeId={}", storeId, e);
                weatherScoreResult = new WeatherScoreResult(
                        0,
                        List.of("날씨 정보 없음"),
                        "날씨 정보 없음"
                );
            } finally {
                logTiming("weatherScore", weatherStart, storeId);
            }

            long airQualityStart = System.nanoTime();
            try {
                CurrentAirQualityObservation airQuality = currentAirQualityService.getCurrentAirQuality(store);
                airQualityScore = airQualityCalculator.getWeight(airQuality);
                airQualityFactor = createAirQualityFactor(airQualityScore);
                airQualityDetail = createAirQualityDetail(airQuality);
            } catch (AirKoreaApiException | IllegalStateException | IllegalArgumentException e) {
                log.warn("공기질 데이터 처리 실패. 공기질 보정 점수를 제외합니다. storeId={}", storeId, e);
            } finally {
                logTiming("airQualityScore", airQualityStart, storeId);
            }

            long assemblyStart = System.nanoTime();
            try {
                int score = capScore(
                        baseScore +
                        timeDemandLevel.getWeight() +
                        dayDemandLevel.getWeight()+
                        weatherScoreResult.getWeatherScore()+
                        airQualityScore);

                String status = calculateStatus(score);
                String message = createMessage(score);

                String timeFactor = timeDemandLevel.getTimeFactor();
                String timeDescription = timeDemandLevel.getTimeDescription();

                String dayFactor = dayDemandLevel.getDayFactor();
                String dayDescription = dayDemandLevel.getDayDescription();

                String currentWeatherFactor = createWeatherFactor(weatherScoreResult);
                String currentWeatherDescription = weatherScoreResult.getDescription();

                return new ScoreResult(score,
                        status,
                        message,
                        timeFactor,
                        timeDescription,
                        dayFactor,
                        dayDescription,
                        currentWeatherFactor,
                        currentWeatherDescription,
                        airQualityFactor,
                        airQualityDescription,
                        airQualityDetail
                        );
            } finally {
                logTiming("scoreAssembly", assemblyStart, storeId);
            }
        } finally {
            logTiming("scoreTotal", totalStart, storeId);
        }
    }

    private String createWeatherFactor(WeatherScoreResult weatherScoreResult) {
        if (weatherScoreResult.getWeatherScore() <= 0) {
            return "•";
        }

        return "↑";
    }

    private boolean isHoliday(LocalDate date, Long storeId) {
        try {
            return holidayService.isHoliday(date);
        } catch (RuntimeException e) {
            log.warn("공휴일 데이터 처리 실패. 기존 요일 기준으로 점수를 계산합니다. date={}, storeId={}",
                    date,
                    storeId,
                    e);
            return false;
        }
    }

    private String createAirQualityFactor(int airQualityScore) {
        if (airQualityScore <= 0) {
            return "•";
        }

        return "↑";
    }

    private String createAirQualityDetail(CurrentAirQualityObservation airQuality) {
        if (airQuality == null) {
            return "대기질 정보 없음";
        }

        return "미세먼지 " + formatNullableValue(airQuality.getPm10Value())
                + ", 초미세먼지 " + formatNullableValue(airQuality.getPm25Value())
                + ", 오존 " + formatNullableValue(airQuality.getO3Value());
    }

    private String formatNullableValue(Object value) {
        if (value == null) {
            return "정보 없음";
        }

        return value.toString();
    }

    private int capScore(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private String calculateStatus(int score) {
        if (score >= 80) {
            return "상 · 수요 급등 구간";
        }

        if (score >= 40) {
            return "중 · 평시 운영 구간";
        }

        if (score >= 20) {
            return "하 · 수요 둔화 구간";
        }

        return "마감 · 조기 마감 검토";
    }

    private String createMessage(int score) {
        if (score >= 80) {
            return "오늘은 배달 수요가 높을 가능성이 큽니다. 연장 영업과 재료 추가 준비를 고려하세요.";
        }

        if (score >= 40) {
            return "평상시 영업을 유지하세요. 피크 시간대 주문 흐름을 지켜보세요.";
        }

        if (score >= 20) {
            return "현재 수요가 낮은 편입니다. 식자재 선조리와 인력 운영을 보수적으로 가져가세요.";
        }

        return "기대 수요가 매우 낮습니다. 유지 비용을 고려해 조기 마감을 검토하세요.";
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

    private static final Logger log = LoggerFactory.getLogger(ScoreService.class);

}
