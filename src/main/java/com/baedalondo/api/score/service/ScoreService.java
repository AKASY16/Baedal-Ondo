package com.baedalondo.api.score.service;

import com.baedalondo.api.airquality.calculator.AirQualityCalculator;
import com.baedalondo.api.airquality.domain.CurrentAirQualityObservation;
import com.baedalondo.api.airquality.exception.AirKoreaApiException;
import com.baedalondo.api.airquality.service.CurrentAirQualityService;
import com.baedalondo.api.common.ServiceTime;
import com.baedalondo.api.holiday.service.HolidayService;
import com.baedalondo.api.score.dto.ScoreTarget;
import com.baedalondo.api.score.factory.ScoreMessageFactory;
import com.baedalondo.api.score.ScoreResult;
import com.baedalondo.api.score.ScoreCalculationResult;
import com.baedalondo.api.score.calculator.DayWeightCalculator;
import com.baedalondo.api.score.calculator.WeightedScoreCalculator;
import com.baedalondo.api.score.dayweight.DayWeightProvider;
import com.baedalondo.api.score.timeweight.TimeWeightProvider;
import com.baedalondo.api.score.status.DayDemandLevel;
import com.baedalondo.api.score.status.TimeDemandLevel;
import com.baedalondo.api.score.calculator.TimeWeightCalculator;
import com.baedalondo.api.weather.calculator.ForecastWeatherWeightCalculator;
import com.baedalondo.api.weather.domain.ForecastWeatherObservation;
import com.baedalondo.api.weather.domain.WeatherScoreResult;
import com.baedalondo.api.weather.exception.KmaWeatherApiException;
import com.baedalondo.api.weather.service.ForecastWeatherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScoreService {

    private final TimeWeightCalculator timeWeightCalculator;
    private final DayWeightCalculator dayWeightCalculator;
    private final DayWeightProvider dayWeightProvider;
    private final TimeWeightProvider timeWeightProvider;
    private final CurrentAirQualityService currentAirQualityService;
    private final AirQualityCalculator airQualityCalculator;
    private final HolidayService holidayService;
    private final ScoreMessageFactory scoreMessageFactory;
    private final WeightedScoreCalculator weightedScoreCalculator;
    private final ForecastWeatherService forecastWeatherService;
    private final ForecastWeatherWeightCalculator forecastWeatherWeightCalculator;

    public ScoreService(TimeWeightCalculator timeWeightCalculator,
                        DayWeightCalculator dayWeightCalculator,
                        DayWeightProvider dayWeightProvider,
                        TimeWeightProvider timeWeightProvider,
                        CurrentAirQualityService currentAirQualityService,
                        AirQualityCalculator airQualityCalculator,
                        HolidayService holidayService,
                        ScoreMessageFactory scoreMessageFactory,
                        WeightedScoreCalculator weightedScoreCalculator,
                        ForecastWeatherService forecastWeatherService,
                        ForecastWeatherWeightCalculator forecastWeatherWeightCalculator) {
        this.timeWeightCalculator = timeWeightCalculator;
        this.dayWeightCalculator = dayWeightCalculator;
        this.dayWeightProvider = dayWeightProvider;
        this.timeWeightProvider = timeWeightProvider;
        this.currentAirQualityService = currentAirQualityService;
        this.airQualityCalculator = airQualityCalculator;
        this.holidayService = holidayService;
        this.scoreMessageFactory = scoreMessageFactory;
        this.weightedScoreCalculator = weightedScoreCalculator;
        this.forecastWeatherService = forecastWeatherService;
        this.forecastWeatherWeightCalculator = forecastWeatherWeightCalculator;
    }


    private static final int FORECAST_HOURS = 5;

    private static final WeatherScoreResult NO_WEATHER_SCORE = new WeatherScoreResult(
            0,
            List.of("날씨 정보 없음"),
            "날씨 정보 없음"
    );

    /**
     현재 시각의 점수를 계산한다.

     날씨는 실황이 아니라 현재 시각의 예보를 쓴다. 실황은 매시 40분에야 제공되어
     그 전까지는 직전 시각 관측밖에 없고, 그러면 시간대·대기질 점수와 기준 시각이 어긋난다.
     예보를 쓰면 세 요소가 모두 같은 시각을 가리킨다.

     실황은 요청 경로에서 아예 호출하지 않는다. 점수에 쓰이지 않는데 매시 첫 요청이
     기상청 응답을 기다리게 되고, 지난 관측이 필요해지면 ASOS로 소급해 받을 수 있다.
     */
    public ScoreResult calculateCurrentScore(ScoreTarget scoreTarget) {
        Long scoreTargetId = scoreTargetId(scoreTarget);

        // 날짜와 시각을 따로 읽으면 자정 경계에서 서로 다른 날을 가리킬 수 있다.
        LocalDateTime now = ServiceTime.now();
        LocalDate currentDate = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime();

        int airQualityScore = 0;
        String airQualityDescription = "대기질 정보를 확인하지 못했어요";
        String airQualityDetail = "대기질 정보 없음";

        TimeDemandLevel timeDemandLevel = findMarketTimeDemandLevel(scoreTarget, currentTime);
        DayDemandLevel dayDemandLevel =
                dayWeightCalculator.calculate(currentDate, isHoliday(currentDate, scoreTargetId));
        int marketDayWeight = findMarketDayWeight(scoreTarget, currentDate);

        ForecastWeatherObservation weather = null;
        WeatherScoreResult weatherScoreResult = NO_WEATHER_SCORE;

        try {
            weather = findForecastAt(scoreTarget, now.truncatedTo(ChronoUnit.HOURS));

            if (weather == null) {
                log.warn("현재 시각 예보가 없습니다. 날씨 보정 점수를 제외합니다. storeId={}", scoreTargetId);
            } else {
                weatherScoreResult = forecastWeatherWeightCalculator.calculate(weather);
            }
        } catch (KmaWeatherApiException e) {
            log.warn("기상청 API 에러. 날씨 보정 점수를 제외합니다. storeId={}", scoreTargetId, e);
        }

        try {
            CurrentAirQualityObservation airQuality =
                    currentAirQualityService.getCurrentAirQuality(scoreTarget);
            airQualityScore = airQualityCalculator.getWeight(airQuality);
            airQualityDetail = scoreMessageFactory.createAirQualityDetail(airQuality);
            airQualityDescription =
                    scoreMessageFactory.createAirQualityDescription(airQuality, airQualityScore);
        } catch (AirKoreaApiException | IllegalStateException | IllegalArgumentException e) {
            log.warn("공기질 데이터 처리 실패. 공기질 보정 점수를 제외합니다. storeId={}", scoreTargetId, e);
        }

        ScoreCalculationResult scoreCalculationResult = weightedScoreCalculator.calculate(
                timeDemandLevel,
                dayDemandLevel,
                marketDayWeight,
                weatherScoreResult,
                weather,
                airQualityScore
        );

        return createScoreResult(
                scoreTarget,
                now,
                timeDemandLevel,
                dayDemandLevel,
                weatherScoreResult,
                scoreCalculationResult,
                airQualityDescription,
                airQualityDetail
        );
    }

    private ForecastWeatherObservation findForecastAt(ScoreTarget scoreTarget, LocalDateTime at) {
        return forecastWeatherService.getForecastWeather(scoreTarget).stream()
                .filter(forecast -> at.equals(forecast.getForecastAt()))
                .findFirst()
                .orElse(null);
    }

    /**
     현재 시각 다음부터 5시간의 시각별 점수를 계산한다.
     현재 시각 점수는 calculateCurrentScore가 따로 내므로 화면은 항상 6칸이 된다.
     예보 조회에 실패하면 빈 Map을 돌려준다.
     */
    public Map<LocalDateTime, ScoreResult> calculateForecastScore(ScoreTarget scoreTarget) {
        Long scoreTargetId = scoreTargetId(scoreTarget);

        List<ForecastWeatherObservation> forecastWeather = List.of();
        Map<LocalDateTime, WeatherScoreResult> weatherScoreResults = Map.of();

        try {
            forecastWeather = selectUpcomingForecasts(
                    forecastWeatherService.getForecastWeather(scoreTarget));
            weatherScoreResults = forecastWeatherWeightCalculator.calculateAll(forecastWeather);
        } catch (KmaWeatherApiException e) {
            log.warn("기상청 API 에러. 미래 날씨 정보를 제외합니다. storeId={}", scoreTargetId, e);
        }

        // 대기질 예보는 일 단위라 시간별 점수에 쓸 수 없다. 현재 값을 6시간까지 그대로 사용한다.
        int airQualityScore = 0;
        String airQualityDescription = "대기질 정보를 확인하지 못했어요";
        String airQualityDetail = "대기질 정보 없음";

        try {
            CurrentAirQualityObservation airQuality =
                    currentAirQualityService.getCurrentAirQuality(scoreTarget);
            airQualityScore = airQualityCalculator.getWeight(airQuality);
            airQualityDetail = scoreMessageFactory.createAirQualityDetail(airQuality);
            airQualityDescription =
                    scoreMessageFactory.createAirQualityDescription(airQuality, airQualityScore);
        } catch (AirKoreaApiException | IllegalStateException | IllegalArgumentException e) {
            log.warn("공기질 데이터 처리 실패. 공기질 보정 점수를 제외합니다. storeId={}", scoreTargetId, e);
        }

        // 공휴일 조회는 날짜당 한 번만 한다. 자정을 넘겨도 날짜는 최대 두 개다.
        Map<LocalDate, Boolean> holidayByDate = new HashMap<>();
        Map<LocalDateTime, ScoreResult> forecastScores = new LinkedHashMap<>();

        for (ForecastWeatherObservation forecast : forecastWeather) {
            LocalDateTime forecastAt = forecast.getForecastAt();
            LocalDate forecastDate = forecastAt.toLocalDate();

            TimeDemandLevel timeDemandLevel =
                    findMarketTimeDemandLevel(scoreTarget, forecastAt.toLocalTime());

            boolean holiday = holidayByDate.computeIfAbsent(
                    forecastDate, date -> isHoliday(date, scoreTargetId));

            DayDemandLevel dayDemandLevel = dayWeightCalculator.calculate(forecastDate, holiday);
            int marketDayWeight = findMarketDayWeight(scoreTarget, forecastDate);

            WeatherScoreResult weatherScoreResult =
                    weatherScoreResults.getOrDefault(forecastAt, NO_WEATHER_SCORE);

            ScoreCalculationResult scoreCalculationResult = weightedScoreCalculator.calculate(
                    timeDemandLevel,
                    dayDemandLevel,
                    marketDayWeight,
                    weatherScoreResult,
                    // 이번 시각의 예보 한 건이다. List 전체를 넘기면 안 된다.
                    forecast,
                    airQualityScore
            );

            forecastScores.put(forecastAt, createScoreResult(
                    scoreTarget,
                    forecastAt,
                    timeDemandLevel,
                    dayDemandLevel,
                    weatherScoreResult,
                    scoreCalculationResult,
                    airQualityDescription,
                    airQualityDetail
            ));
        }

        return forecastScores;
    }

    /**
     현재 시각 이후의 예보만 남긴다.

     기준 발표분이 항상 직전 시각 :30이므로 응답은 현재 시각부터 6시간이다.
     현재 시각 항목은 calculateCurrentScore가 쓰므로 여기서는 제외되고, 남는 것은 언제나 5개다.
     */
    private List<ForecastWeatherObservation> selectUpcomingForecasts(
            List<ForecastWeatherObservation> forecasts) {
        LocalDateTime now = ServiceTime.now();

        return forecasts.stream()
                .filter(forecast -> forecast.getForecastAt().isAfter(now))
                .sorted(Comparator.comparing(ForecastWeatherObservation::getForecastAt))
                .limit(FORECAST_HOURS)
                .toList();
    }

    /**
     계산 결과를 화면 표시용 ScoreResult로 조립한다.
     현재 점수와 예보 점수가 같은 문구 규칙을 쓰므로 한 곳에 둔다.
     문구를 바꿀 때 두 경로가 어긋나는 것을 막으려는 목적이다.
     */
    private ScoreResult createScoreResult(ScoreTarget scoreTarget,
                                          LocalDateTime at,
                                          TimeDemandLevel timeDemandLevel,
                                          DayDemandLevel dayDemandLevel,
                                          WeatherScoreResult weatherScoreResult,
                                          ScoreCalculationResult calculationResult,
                                          String airQualityDescription,
                                          String airQualityDetail) {
        int score = calculationResult.score();

        String businessTypeName = scoreTarget != null && scoreTarget.getBusinessType() != null
                ? scoreTarget.getBusinessType().getDisplayName()
                : null;

        return new ScoreResult(
                score,
                scoreMessageFactory.calculateStatus(score),
                scoreMessageFactory.createMessage(score),
                timeDemandLevel.getTimeFactor(),
                scoreMessageFactory.createTimeDescription(timeDemandLevel, at.toLocalTime()),
                // 화살표는 실제 적용된 요일 점수 기준이어야 한다.
                // 상권에 따라 주말도 음수가 될 수 있어 enum의 고정 화살표를 쓰면 표시가 어긋난다.
                scoreMessageFactory.createDayFactor(calculationResult.dayScore()),
                scoreMessageFactory.createLocalPatternDescription(
                        dayDemandLevel,
                        calculationResult.dayScore(),
                        businessTypeName,
                        at.getDayOfWeek()
                ),
                scoreMessageFactory.createWeatherFactor(weatherScoreResult),
                weatherScoreResult.getDescription(),
                scoreMessageFactory.createAirQualityFactor(calculationResult.airQualityScore()),
                airQualityDescription,
                airQualityDetail
        );
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

    /**
     상권 x 업종 x 시간대 등급을 우선 사용한다.
     업종이 없는 게스트이거나 TimeWeight가 없으면 기존 공통 시간표를 사용한다.
     */
    private TimeDemandLevel findMarketTimeDemandLevel(ScoreTarget scoreTarget, LocalTime time) {
        if (scoreTarget != null) {
            TimeDemandLevel marketLevel = timeWeightProvider.findDemandLevel(
                    scoreTarget.getCommercialAreaCode(),
                    scoreTarget.getBusinessType(),
                    time
            );

            if (marketLevel != null) {
                return marketLevel;
            }
        }

        return timeWeightCalculator.calculate(time);
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

    private Long scoreTargetId(ScoreTarget scoreTarget) {
        return scoreTarget == null ? null : scoreTarget.getId();
    }

    private static final Logger log = LoggerFactory.getLogger(ScoreService.class);

}
