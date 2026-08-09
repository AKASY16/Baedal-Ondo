package com.baedalondo.api.score.service;

import com.baedalondo.api.airquality.calculator.AirQualityCalculator;
import com.baedalondo.api.airquality.domain.CurrentAirQualityObservation;
import com.baedalondo.api.airquality.exception.AirKoreaApiException;
import com.baedalondo.api.airquality.service.CurrentAirQualityService;
import com.baedalondo.api.holiday.service.HolidayService;
import com.baedalondo.api.score.dto.ScoreTarget;
import com.baedalondo.api.score.factory.ScoreMessageFactory;
import com.baedalondo.api.score.ScoreResult;
import com.baedalondo.api.score.ScoreCalculationResult;
import com.baedalondo.api.score.calculator.DayWeightCalculator;
import com.baedalondo.api.score.calculator.WeightedScoreCalculator;
import com.baedalondo.api.score.dayweight.DayWeightProvider;
import com.baedalondo.api.score.status.DayDemandLevel;
import com.baedalondo.api.score.status.TimeDemandLevel;
import com.baedalondo.api.score.calculator.TimeWeightCalculator;
import com.baedalondo.api.weather.calculator.CurrentWeatherWeightCalculator;
import com.baedalondo.api.weather.domain.CurrentWeatherObservation;
import com.baedalondo.api.weather.domain.WeatherScoreResult;
import com.baedalondo.api.weather.exception.KmaWeatherApiException;
import com.baedalondo.api.weather.service.CurrentWeatherService;
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
    private final DayWeightProvider dayWeightProvider;
    private final CurrentWeatherWeightCalculator currentWeatherWeightCalculator;
    private final CurrentWeatherService currentWeatherService;
    private final CurrentAirQualityService currentAirQualityService;
    private final AirQualityCalculator airQualityCalculator;
    private final HolidayService holidayService;
    private final ScoreMessageFactory scoreMessageFactory;
    private final WeightedScoreCalculator weightedScoreCalculator;

    public ScoreService(TimeWeightCalculator timeWeightCalculator,
                        DayWeightCalculator dayWeightCalculator,
                        DayWeightProvider dayWeightProvider,
                        CurrentWeatherWeightCalculator currentWeatherWeightCalculator,
                        CurrentWeatherService currentWeatherService,
                        CurrentAirQualityService currentAirQualityService,
                        AirQualityCalculator airQualityCalculator,
                        HolidayService holidayService,
                        ScoreMessageFactory scoreMessageFactory,
                        WeightedScoreCalculator weightedScoreCalculator) {
        this.timeWeightCalculator = timeWeightCalculator;
        this.dayWeightCalculator = dayWeightCalculator;
        this.dayWeightProvider = dayWeightProvider;
        this.currentWeatherWeightCalculator = currentWeatherWeightCalculator;
        this.currentWeatherService = currentWeatherService;
        this.currentAirQualityService = currentAirQualityService;
        this.airQualityCalculator = airQualityCalculator;
        this.holidayService = holidayService;
        this.scoreMessageFactory = scoreMessageFactory;
        this.weightedScoreCalculator = weightedScoreCalculator;
    }


    public ScoreResult calculateCurrentScore(ScoreTarget scoreTarget) {
        long totalStart = System.nanoTime();
        Long scoreTargetId = scoreTargetId(scoreTarget);

        try {
            int airQualityScore = 0;
            String airQualityFactor = "영향 없음";
            String airQualityDescription = "대기질";
            String airQualityDetail = "대기질 정보 없음";
            CurrentWeatherObservation weather = null;

            TimeDemandLevel timeDemandLevel;
            DayDemandLevel dayDemandLevel;
            int marketDayWeight;
            long baseStart = System.nanoTime();
            try {
                LocalDate currentDate = LocalDate.now();
                timeDemandLevel = timeWeightCalculator.calculate(LocalTime.now());
                dayDemandLevel = dayWeightCalculator.calculate(currentDate, isHoliday(currentDate, scoreTargetId));
                marketDayWeight = findMarketDayWeight(scoreTarget, currentDate);
            } finally {
                logTiming("baseScore", baseStart, scoreTargetId);
            }

            WeatherScoreResult weatherScoreResult;
            long weatherStart = System.nanoTime();
            try {
                weather = currentWeatherService.getCurrentWeather(scoreTarget);
                weatherScoreResult = currentWeatherWeightCalculator.calculate(weather);
            } catch (KmaWeatherApiException e) {
                log.warn("기상청 API 에러. 날씨 보정 점수를 제외합니다. storeId={}", scoreTargetId, e);
                weatherScoreResult = new WeatherScoreResult(
                        0,
                        List.of("날씨 정보 없음"),
                        "날씨 정보 없음"
                );
            } finally {
                logTiming("weatherScore", weatherStart, scoreTargetId);
            }

            long airQualityStart = System.nanoTime();
            try {
                CurrentAirQualityObservation airQuality = currentAirQualityService.getCurrentAirQuality(scoreTarget);
                airQualityScore = airQualityCalculator.getWeight(airQuality);
                airQualityFactor = scoreMessageFactory.createAirQualityFactor(airQualityScore);
                airQualityDetail = scoreMessageFactory.createAirQualityDetail(airQuality);
            } catch (AirKoreaApiException | IllegalStateException | IllegalArgumentException e) {
                log.warn("공기질 데이터 처리 실패. 공기질 보정 점수를 제외합니다. storeId={}", scoreTargetId, e);
            } finally {
                logTiming("airQualityScore", airQualityStart, scoreTargetId);
            }

            long assemblyStart = System.nanoTime();
            try {
                ScoreCalculationResult scoreCalculationResult = weightedScoreCalculator.calculate(
                        timeDemandLevel,
                        dayDemandLevel,
                        marketDayWeight,
                        weatherScoreResult,
                        weather,
                        airQualityScore
                );
                int score = scoreCalculationResult.score();

                String status = scoreMessageFactory.calculateStatus(score);
                String message = scoreMessageFactory.createMessage(score);

                String timeFactor = timeDemandLevel.getTimeFactor();
                String timeDescription = timeDemandLevel.getTimeDescription();

                // 화살표는 실제 적용된 요일 점수 기준이어야 한다.
                // 상권에 따라 주말도 음수가 될 수 있어 enum의 고정 화살표를 쓰면 표시가 어긋난다.
                String dayFactor = scoreMessageFactory.createDayFactor(scoreCalculationResult.dayScore());
                String dayDescription = dayDemandLevel.getDayDescription();

                String currentWeatherFactor = scoreMessageFactory.createWeatherFactor(weatherScoreResult);
                String currentWeatherDescription = weatherScoreResult.getDescription();
                airQualityFactor = scoreMessageFactory.createAirQualityFactor(scoreCalculationResult.airQualityScore());

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
                logTiming("scoreAssembly", assemblyStart, scoreTargetId);
            }
        } finally {
            logTiming("scoreTotal", totalStart, scoreTargetId);
        }
    }



    /**
     상권 x 업종 x 요일 가중치를 조회한다.
     Local -> City -> 0 fallback은 DayWeightProvider가 처리한다.
     */
    private int findMarketDayWeight(ScoreTarget scoreTarget, LocalDate date) {
        if (scoreTarget == null) {
            return 0;
        }

        return dayWeightProvider.findWeight(
                scoreTarget.getCommercialAreaCode(),
                scoreTarget.getBusinessType(),
                date.getDayOfWeek()
        );
    }

    private boolean isHoliday(LocalDate date, Long scoreTargetId) {
        try {
            return holidayService.isHoliday(date);
        } catch (RuntimeException e) {
            log.warn("공휴일 데이터 처리 실패. 기존 요일 기준으로 점수를 계산합니다. date={}, storeId={}",
                    date,
                    scoreTargetId,
                    e);
            return false;
        }
    }

    private void logTiming(String step, long startNanos, Long scoreTargetId) {
        log.info("dashboard timing step={} elapsedMs={} storeId={}",
                step,
                elapsedMs(startNanos),
                scoreTargetId);
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private Long scoreTargetId(ScoreTarget scoreTarget) {
        return scoreTarget == null ? null : scoreTarget.getId();
    }

    private static final Logger log = LoggerFactory.getLogger(ScoreService.class);

}
