package com.baedalondo.api.score.service;

import com.baedalondo.api.airquality.calculator.AirQualityCalculator;
import com.baedalondo.api.airquality.domain.CurrentAirQualityObservation;
import com.baedalondo.api.airquality.exception.AirKoreaApiException;
import com.baedalondo.api.airquality.service.CurrentAirQualityService;
import com.baedalondo.api.holiday.service.HolidayService;
import com.baedalondo.api.score.ScoreResult;
import com.baedalondo.api.score.calculator.DayWeightCalculator;
import com.baedalondo.api.score.dayweight.DayWeightProvider;
import com.baedalondo.api.score.timeweight.TimeWeightProvider;
import com.baedalondo.api.store.domain.BusinessType;
import com.baedalondo.api.score.calculator.TimeWeightCalculator;
import com.baedalondo.api.score.calculator.WeightedScoreCalculator;
import com.baedalondo.api.score.dto.ScoreTarget;
import com.baedalondo.api.score.factory.ScoreMessageFactory;
import com.baedalondo.api.score.status.DayDemandLevel;
import com.baedalondo.api.score.status.TimeDemandLevel;
import com.baedalondo.api.weather.calculator.ForecastWeatherWeightCalculator;
import com.baedalondo.api.weather.domain.ForecastWeatherObservation;
import com.baedalondo.api.weather.domain.WeatherScoreResult;
import com.baedalondo.api.weather.exception.KmaWeatherApiException;
import com.baedalondo.api.weather.service.ForecastWeatherService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScoreServiceTest {

    private static final String COMMERCIAL_AREA_CODE = "3120029";
    private static final LocalDateTime REFERENCE_TIME =
            LocalDateTime.of(2026, 8, 22, 13, 30);

    @Mock
    private TimeWeightCalculator timeWeightCalculator;

    @Mock
    private DayWeightCalculator dayWeightCalculator;

    @Mock
    private DayWeightProvider dayWeightProvider;

    @Mock
    private TimeWeightProvider timeWeightProvider;

    @Mock
    private CurrentAirQualityService currentAirQualityService;

    @Mock
    private AirQualityCalculator airQualityCalculator;

    @Mock
    private HolidayService holidayService;

    @Mock
    private ScoreMessageFactory scoreMessageFactory;

    @Mock
    private ForecastWeatherService forecastWeatherService;

    @Mock
    private ForecastWeatherWeightCalculator forecastWeatherWeightCalculator;

    @Spy
    private WeightedScoreCalculator weightedScoreCalculator = new WeightedScoreCalculator();

    @InjectMocks
    private ScoreService scoreService;

    @Test
    void airQualityScoreTest() {
        // given
        ScoreTarget scoreTarget = createScoreTarget();
        CurrentAirQualityObservation airQuality = createAirQuality();

        when(holidayService.isHoliday(any(LocalDate.class))).thenReturn(false);
        when(timeWeightCalculator.calculate(any(LocalTime.class)))
                .thenReturn(createNoImpactTime());
        when(dayWeightCalculator.calculate(any(LocalDate.class), anyBoolean()))
                .thenReturn(DayDemandLevel.WEEKDAY); // 0점
        when(forecastWeatherService.getForecastWeather(any(ScoreTarget.class), any(LocalDateTime.class)))
                .thenReturn(List.of(createCurrentHourForecast()));
        when(forecastWeatherWeightCalculator.calculate(any(ForecastWeatherObservation.class)))
                .thenReturn(new WeatherScoreResult(0, List.of(), "날씨 영향 없음"));
        when(currentAirQualityService.getCurrentAirQuality(any(ScoreTarget.class), any(LocalDateTime.class)))
                .thenReturn(airQuality);
        when(airQualityCalculator.getWeight(any(CurrentAirQualityObservation.class)))
                .thenReturn(5);

        // when
        ScoreResult result = scoreService.calculateCurrentScore(scoreTarget, REFERENCE_TIME);

        // then
        assertEquals(58, result.getScore());
    }

    @Test
    void timeWeightScoreTest() {
        ScoreTarget scoreTarget = createScoreTarget();
        CurrentAirQualityObservation airQuality = createAirQuality();

        when(holidayService.isHoliday(any(LocalDate.class))).thenReturn(false);
        when(timeWeightCalculator.calculate(any(LocalTime.class)))
                .thenReturn(TimeDemandLevel.LOW); // -6점
        when(dayWeightCalculator.calculate(any(LocalDate.class), anyBoolean()))
                .thenReturn(DayDemandLevel.WEEKDAY); // 0점
        when(forecastWeatherService.getForecastWeather(any(ScoreTarget.class), any(LocalDateTime.class)))
                .thenReturn(List.of(createCurrentHourForecast()));
        when(forecastWeatherWeightCalculator.calculate(any(ForecastWeatherObservation.class)))
                .thenReturn(new WeatherScoreResult(0, List.of(), "날씨 영향 없음"));
        when(currentAirQualityService.getCurrentAirQuality(any(ScoreTarget.class), any(LocalDateTime.class)))
                .thenReturn(airQuality);
        when(airQualityCalculator.getWeight(any(CurrentAirQualityObservation.class)))
                .thenReturn(0);

        ScoreResult result = scoreService.calculateCurrentScore(scoreTarget, REFERENCE_TIME);

        assertEquals(44, result.getScore());
    }

    @Test
    @DisplayName("Store의 상권과 업종에 맞는 시간대 등급을 공통 시간표보다 우선한다")
    void marketTimeLevelTakesPrecedenceOverLegacyTimeTable() {
        ScoreTarget scoreTarget = createScoreTarget();
        CurrentAirQualityObservation airQuality = createAirQuality();

        when(holidayService.isHoliday(any(LocalDate.class))).thenReturn(false);
        when(timeWeightProvider.findDemandLevel(eq(COMMERCIAL_AREA_CODE),
                eq(BusinessType.CHICKEN), any(LocalTime.class)))
                .thenReturn(TimeDemandLevel.VERY_HIGH);
        when(dayWeightCalculator.calculate(any(LocalDate.class), anyBoolean()))
                .thenReturn(DayDemandLevel.WEEKDAY);
        when(forecastWeatherService.getForecastWeather(any(ScoreTarget.class), any(LocalDateTime.class)))
                .thenReturn(List.of(createCurrentHourForecast()));
        when(forecastWeatherWeightCalculator.calculate(any(ForecastWeatherObservation.class)))
                .thenReturn(new WeatherScoreResult(0, List.of(), "날씨 영향 없음"));
        when(currentAirQualityService.getCurrentAirQuality(any(ScoreTarget.class), any(LocalDateTime.class)))
                .thenReturn(airQuality);
        when(airQualityCalculator.getWeight(any(CurrentAirQualityObservation.class)))
                .thenReturn(0);

        ScoreResult result = scoreService.calculateCurrentScore(scoreTarget, REFERENCE_TIME);

        assertEquals(64, result.getScore());
        verify(timeWeightProvider).findDemandLevel(
                eq(COMMERCIAL_AREA_CODE), eq(BusinessType.CHICKEN), any(LocalTime.class));
        verify(timeWeightCalculator, org.mockito.Mockito.never()).calculate(any(LocalTime.class));
    }

    @Test
    @DisplayName("상권 DayWeight가 요일 점수로 그대로 적용된다")
    void dayWeightScoreTest() {
        ScoreTarget scoreTarget = createScoreTarget();
        CurrentAirQualityObservation airQuality = createAirQuality();

        when(holidayService.isHoliday(any(LocalDate.class))).thenReturn(false);
        when(timeWeightCalculator.calculate(any(LocalTime.class)))
                .thenReturn(createNoImpactTime()); // 0점
        when(dayWeightCalculator.calculate(any(LocalDate.class), anyBoolean()))
                .thenReturn(DayDemandLevel.WEEKEND);
        // 주말 고정 +8이 아니라 상권 값 +6이 적용되어야 한다
        when(dayWeightProvider.findWeight(any(), any(), any()))
                .thenReturn(6);
        when(forecastWeatherService.getForecastWeather(any(ScoreTarget.class), any(LocalDateTime.class)))
                .thenReturn(List.of(createCurrentHourForecast()));
        when(forecastWeatherWeightCalculator.calculate(any(ForecastWeatherObservation.class)))
                .thenReturn(new WeatherScoreResult(0, List.of(), "날씨 영향 없음"));
        when(currentAirQualityService.getCurrentAirQuality(any(ScoreTarget.class), any(LocalDateTime.class)))
                .thenReturn(airQuality);
        when(airQualityCalculator.getWeight(any(CurrentAirQualityObservation.class)))
                .thenReturn(0);

        ScoreResult result = scoreService.calculateCurrentScore(scoreTarget, REFERENCE_TIME);

        assertEquals(56, result.getScore());
    }

    @Test
    @DisplayName("상권 DayWeight가 음수면 주말이라도 점수가 내려간다")
    void negativeDayWeightScoreTest() {
        ScoreTarget scoreTarget = createScoreTarget();
        CurrentAirQualityObservation airQuality = createAirQuality();

        when(holidayService.isHoliday(any(LocalDate.class))).thenReturn(false);
        when(timeWeightCalculator.calculate(any(LocalTime.class)))
                .thenReturn(createNoImpactTime());
        when(dayWeightCalculator.calculate(any(LocalDate.class), anyBoolean()))
                .thenReturn(DayDemandLevel.WEEKEND);
        when(dayWeightProvider.findWeight(any(), any(), any()))
                .thenReturn(-6);
        when(forecastWeatherService.getForecastWeather(any(ScoreTarget.class), any(LocalDateTime.class)))
                .thenReturn(List.of(createCurrentHourForecast()));
        when(forecastWeatherWeightCalculator.calculate(any(ForecastWeatherObservation.class)))
                .thenReturn(new WeatherScoreResult(0, List.of(), "날씨 영향 없음"));
        when(currentAirQualityService.getCurrentAirQuality(any(ScoreTarget.class), any(LocalDateTime.class)))
                .thenReturn(airQuality);
        when(airQualityCalculator.getWeight(any(CurrentAirQualityObservation.class)))
                .thenReturn(0);

        ScoreResult result = scoreService.calculateCurrentScore(scoreTarget, REFERENCE_TIME);

        assertEquals(44, result.getScore());
    }

    @Test
    @DisplayName("Store의 상권코드와 업종, 오늘 요일로 DayWeight를 조회한다")
    void passesStoreKeysToDayWeightProvider() {
        ScoreTarget scoreTarget = createScoreTarget();
        stubCalmDependencies(DayDemandLevel.WEEKDAY);

        scoreService.calculateCurrentScore(scoreTarget, REFERENCE_TIME);

        verify(dayWeightProvider).findWeight(
                COMMERCIAL_AREA_CODE,
                BusinessType.CHICKEN,
                REFERENCE_TIME.getDayOfWeek());
    }

    @Test
    @DisplayName("게스트 지역은 상권과 업종 없이 조회되어 요일 점수가 0이 된다")
    void guestTargetHasNoMarketDayWeight() {
        ScoreTarget guestTarget = createGuestScoreTarget();
        stubCalmDependencies(DayDemandLevel.WEEKEND);

        ScoreResult result = scoreService.calculateCurrentScore(guestTarget, REFERENCE_TIME);

        verify(dayWeightProvider).findWeight(null, null, REFERENCE_TIME.getDayOfWeek());
        // 게스트는 주말이어도 기존 +8을 받지 않는다.
        assertEquals(50, result.getScore());
    }

    @Test
    void holidayScoreTest() {
        ScoreTarget scoreTarget = createScoreTarget();
        CurrentAirQualityObservation airQuality = createAirQuality();

        when(holidayService.isHoliday(any(LocalDate.class))).thenReturn(true);
        when(timeWeightCalculator.calculate(any(LocalTime.class)))
                .thenReturn(createNoImpactTime()); // 0점
        when(dayWeightCalculator.calculate(any(LocalDate.class), eq(true)))
                .thenReturn(DayDemandLevel.HOLIDAY); // +8점
        // 공휴일에는 상권 DayWeight를 더하지 않으므로 이 값은 무시되어야 한다.
        when(dayWeightProvider.findWeight(any(), any(), any()))
                .thenReturn(6);
        when(forecastWeatherService.getForecastWeather(any(ScoreTarget.class), any(LocalDateTime.class)))
                .thenReturn(List.of(createCurrentHourForecast()));
        when(forecastWeatherWeightCalculator.calculate(any(ForecastWeatherObservation.class)))
                .thenReturn(new WeatherScoreResult(0, List.of(), "날씨 영향 없음"));
        when(currentAirQualityService.getCurrentAirQuality(any(ScoreTarget.class), any(LocalDateTime.class)))
                .thenReturn(airQuality);
        when(airQualityCalculator.getWeight(any(CurrentAirQualityObservation.class)))
                .thenReturn(0);

        ScoreResult result = scoreService.calculateCurrentScore(scoreTarget, REFERENCE_TIME);

        assertEquals(58, result.getScore());
    }

    @Test
    void weatherScoreTest() {
        ScoreTarget scoreTarget = createScoreTarget();
        CurrentAirQualityObservation airQuality = createAirQuality();

        when(holidayService.isHoliday(any(LocalDate.class))).thenReturn(false);
        when(timeWeightCalculator.calculate(any(LocalTime.class)))
                .thenReturn(createNoImpactTime()); // 0점
        when(dayWeightCalculator.calculate(any(LocalDate.class), anyBoolean()))
                .thenReturn(DayDemandLevel.WEEKDAY); // 0점
        when(forecastWeatherService.getForecastWeather(any(ScoreTarget.class), any(LocalDateTime.class)))
                .thenReturn(List.of(createCurrentHourForecast()));
        when(forecastWeatherWeightCalculator.calculate(any(ForecastWeatherObservation.class)))
                .thenReturn(new WeatherScoreResult(4, List.of("풍속"), "풍속")); // +4점
        when(currentAirQualityService.getCurrentAirQuality(any(ScoreTarget.class), any(LocalDateTime.class)))
                .thenReturn(airQuality);
        when(airQualityCalculator.getWeight(any(CurrentAirQualityObservation.class)))
                .thenReturn(0);

        ScoreResult result = scoreService.calculateCurrentScore(scoreTarget, REFERENCE_TIME);

        assertEquals(55, result.getScore());
    }

    @Test
    void over100ScoreTest() {
        ScoreTarget scoreTarget = createScoreTarget();
        CurrentAirQualityObservation airQuality = createAirQuality();

        when(holidayService.isHoliday(any(LocalDate.class))).thenReturn(false);
        when(timeWeightCalculator.calculate(any(LocalTime.class)))
                .thenReturn(TimeDemandLevel.VERY_HIGH);
        when(dayWeightCalculator.calculate(any(LocalDate.class), anyBoolean()))
                .thenReturn(DayDemandLevel.WEEKEND);
        when(dayWeightProvider.findWeight(any(), any(), any()))
                .thenReturn(6);
        when(forecastWeatherService.getForecastWeather(any(ScoreTarget.class), any(LocalDateTime.class)))
                .thenReturn(List.of(createCurrentHourForecast()));
        when(forecastWeatherWeightCalculator.calculate(any(ForecastWeatherObservation.class)))
                .thenReturn(new WeatherScoreResult(100, List.of("100점 초과 테스트"), "100점 초과 테스트")); // +100점
        when(currentAirQualityService.getCurrentAirQuality(any(ScoreTarget.class), any(LocalDateTime.class)))
                .thenReturn(airQuality);
        when(airQualityCalculator.getWeight(any(CurrentAirQualityObservation.class)))
                .thenReturn(7);

        ScoreResult result = scoreService.calculateCurrentScore(scoreTarget, REFERENCE_TIME);

        assertEquals(100, result.getScore());
    }

    @Test
    void weatherApiExceptionScoreTest() {
        ScoreTarget scoreTarget = createScoreTarget();
        CurrentAirQualityObservation airQuality = createAirQuality();

        when(holidayService.isHoliday(any(LocalDate.class))).thenReturn(false);
        when(timeWeightCalculator.calculate(any(LocalTime.class)))
                .thenReturn(createNoImpactTime()); // 0점
        when(dayWeightCalculator.calculate(any(LocalDate.class), anyBoolean()))
                .thenReturn(DayDemandLevel.WEEKDAY); // 0점
        when(forecastWeatherService.getForecastWeather(any(ScoreTarget.class), any(LocalDateTime.class)))
                .thenThrow(new KmaWeatherApiException("날씨 API 테스트 예외"));
        when(currentAirQualityService.getCurrentAirQuality(any(ScoreTarget.class), any(LocalDateTime.class)))
                .thenReturn(airQuality);
        when(airQualityCalculator.getWeight(any(CurrentAirQualityObservation.class)))
                .thenReturn(0);

        ScoreResult result = scoreService.calculateCurrentScore(scoreTarget, REFERENCE_TIME);

        assertEquals(50, result.getScore());
    }

    @Test
    void airQualityApiExceptionScoreTest() {
        ScoreTarget scoreTarget = createScoreTarget();

        when(holidayService.isHoliday(any(LocalDate.class))).thenReturn(false);
        when(timeWeightCalculator.calculate(any(LocalTime.class)))
                .thenReturn(createNoImpactTime()); // 0점
        when(dayWeightCalculator.calculate(any(LocalDate.class), anyBoolean()))
                .thenReturn(DayDemandLevel.WEEKDAY); // 0점
        when(forecastWeatherService.getForecastWeather(any(ScoreTarget.class), any(LocalDateTime.class)))
                .thenReturn(List.of(createCurrentHourForecast()));
        when(forecastWeatherWeightCalculator.calculate(any(ForecastWeatherObservation.class)))
                .thenReturn(new WeatherScoreResult(0, List.of(), "날씨 영향 없음"));
        when(currentAirQualityService.getCurrentAirQuality(any(ScoreTarget.class), any(LocalDateTime.class)))
                .thenThrow(new AirKoreaApiException("공기질 API 테스트 예외"));

        ScoreResult result = scoreService.calculateCurrentScore(scoreTarget, REFERENCE_TIME);

        assertEquals(50, result.getScore());
    }

    @Test
    void holidayExceptionScoreTest() {
        ScoreTarget scoreTarget = createScoreTarget();
        CurrentAirQualityObservation airQuality = createAirQuality();

        when(holidayService.isHoliday(any(LocalDate.class)))
                .thenThrow(new RuntimeException("공휴일 조회 테스트 예외"));
        when(timeWeightCalculator.calculate(any(LocalTime.class)))
                .thenReturn(createNoImpactTime()); // 0점
        when(dayWeightCalculator.calculate(any(LocalDate.class), eq(false)))
                .thenReturn(DayDemandLevel.WEEKDAY); // 0점
        when(forecastWeatherService.getForecastWeather(any(ScoreTarget.class), any(LocalDateTime.class)))
                .thenReturn(List.of(createCurrentHourForecast()));
        when(forecastWeatherWeightCalculator.calculate(any(ForecastWeatherObservation.class)))
                .thenReturn(new WeatherScoreResult(0, List.of(), "날씨 영향 없음"));
        when(currentAirQualityService.getCurrentAirQuality(any(ScoreTarget.class), any(LocalDateTime.class)))
                .thenReturn(airQuality);
        when(airQualityCalculator.getWeight(any(CurrentAirQualityObservation.class)))
                .thenReturn(0);

        ScoreResult result = scoreService.calculateCurrentScore(scoreTarget, REFERENCE_TIME);

        assertEquals(50, result.getScore());
    }


    /** 시간/날씨/대기질 영향이 전혀 없는 상태로 만들어 요일 점수만 관찰한다. */
    private void stubCalmDependencies(DayDemandLevel dayDemandLevel) {
        when(holidayService.isHoliday(any(LocalDate.class))).thenReturn(false);
        when(timeWeightCalculator.calculate(any(LocalTime.class)))
                .thenReturn(createNoImpactTime());
        when(dayWeightCalculator.calculate(any(LocalDate.class), anyBoolean()))
                .thenReturn(dayDemandLevel);
        when(forecastWeatherService.getForecastWeather(any(ScoreTarget.class), any(LocalDateTime.class)))
                .thenReturn(List.of(createCurrentHourForecast()));
        when(forecastWeatherWeightCalculator.calculate(any(ForecastWeatherObservation.class)))
                .thenReturn(new WeatherScoreResult(0, List.of(), "날씨 영향 없음"));
        when(currentAirQualityService.getCurrentAirQuality(any(ScoreTarget.class), any(LocalDateTime.class)))
                .thenReturn(createAirQuality());
        when(airQualityCalculator.getWeight(any(CurrentAirQualityObservation.class)))
                .thenReturn(0);
    }

    @Test
    @DisplayName("현재 시각 예보가 없으면 날씨 보정 없이 계산한다")
    void skipsWeatherWhenCurrentHourForecastMissing() {
        ScoreTarget scoreTarget = createScoreTarget();
        LocalDateTime base = REFERENCE_TIME.truncatedTo(ChronoUnit.HOURS);

        when(holidayService.isHoliday(any(LocalDate.class))).thenReturn(false);
        when(timeWeightCalculator.calculate(any(LocalTime.class)))
                .thenReturn(createNoImpactTime());
        when(dayWeightCalculator.calculate(any(LocalDate.class), anyBoolean()))
                .thenReturn(DayDemandLevel.WEEKDAY);
        when(forecastWeatherService.getForecastWeather(any(ScoreTarget.class), any(LocalDateTime.class)))
                .thenReturn(List.of(createForecast(base.plusHours(2))));

        ScoreResult result = scoreService.calculateCurrentScore(scoreTarget, REFERENCE_TIME);

        assertEquals(50, result.getScore());
    }

    @Test
    @DisplayName("이미 지난 예보는 계산에서 제외한다")
    void excludesPastForecasts() {
        ScoreTarget scoreTarget = createScoreTarget();
        LocalDateTime base = REFERENCE_TIME.truncatedTo(ChronoUnit.HOURS);

        when(forecastWeatherService.getForecastWeather(any(ScoreTarget.class), any(LocalDateTime.class)))
                .thenReturn(List.of(
                        createForecast(base.minusHours(2)),
                        createForecast(base.minusHours(1)),
                        createForecast(base.plusHours(2)),
                        createForecast(base.plusHours(3))
                ));
        when(holidayService.isHoliday(any(LocalDate.class))).thenReturn(false);
        when(timeWeightCalculator.calculate(any(LocalTime.class)))
                .thenReturn(createNoImpactTime());
        when(dayWeightCalculator.calculate(any(LocalDate.class), anyBoolean()))
                .thenReturn(DayDemandLevel.WEEKDAY);

        Map<LocalDateTime, ScoreResult> result = scoreService.calculateForecastScore(scoreTarget, REFERENCE_TIME);

        assertEquals(2, result.size());
        assertTrue(result.containsKey(base.plusHours(2)));
        assertTrue(result.containsKey(base.plusHours(3)));
    }

    @Test
    @DisplayName("정각 직전에는 현재 시각 다음 시간부터 미래 5칸을 유지한다")
    void keepsSixSlotsImmediatelyBeforeHourBoundary() {
        assertCurrentAndFutureSlots(
                LocalDateTime.of(2026, 8, 22, 13, 59, 59, 900_000_000),
                LocalDateTime.of(2026, 8, 22, 13, 0));
    }

    @Test
    @DisplayName("정각 직후에도 현재 시각 다음 시간부터 미래 5칸을 유지한다")
    void keepsSixSlotsImmediatelyAfterHourBoundary() {
        assertCurrentAndFutureSlots(
                LocalDateTime.of(2026, 8, 22, 14, 0, 0, 100_000_000),
                LocalDateTime.of(2026, 8, 22, 14, 0));
    }

    @Test
    @DisplayName("현재 시각 다음부터 5시간까지만 계산한다")
    void limitsToSixHours() {
        ScoreTarget scoreTarget = createScoreTarget();
        LocalDateTime base = REFERENCE_TIME.truncatedTo(ChronoUnit.HOURS);

        List<ForecastWeatherObservation> forecasts = new ArrayList<>();
        for (int hour = 1; hour <= 9; hour++) {
            forecasts.add(createForecast(base.plusHours(hour)));
        }

        when(forecastWeatherService.getForecastWeather(any(ScoreTarget.class), any(LocalDateTime.class)))
                .thenReturn(forecasts);
        when(holidayService.isHoliday(any(LocalDate.class))).thenReturn(false);
        when(timeWeightCalculator.calculate(any(LocalTime.class)))
                .thenReturn(createNoImpactTime());
        when(dayWeightCalculator.calculate(any(LocalDate.class), anyBoolean()))
                .thenReturn(DayDemandLevel.WEEKDAY);

        Map<LocalDateTime, ScoreResult> result = scoreService.calculateForecastScore(scoreTarget, REFERENCE_TIME);

        assertEquals(5, result.size());
    }

    @Test
    @DisplayName("공휴일 조회는 예보 건수가 아니라 날짜 수만큼만 한다")
    void looksUpHolidayOncePerDate() {
        // 공휴일 조회는 비공휴일마다 외부 API를 호출하는 구조라
        // 예보 6건에 6번 부르면 요청당 왕복이 6배가 된다.
        ScoreTarget scoreTarget = createScoreTarget();
        LocalDateTime base = REFERENCE_TIME.truncatedTo(ChronoUnit.HOURS);

        List<ForecastWeatherObservation> forecasts = new ArrayList<>();
        for (int hour = 1; hour <= 6; hour++) {
            forecasts.add(createForecast(base.plusHours(hour)));
        }

        when(forecastWeatherService.getForecastWeather(any(ScoreTarget.class), any(LocalDateTime.class)))
                .thenReturn(forecasts);
        when(holidayService.isHoliday(any(LocalDate.class))).thenReturn(false);
        when(timeWeightCalculator.calculate(any(LocalTime.class)))
                .thenReturn(createNoImpactTime());
        when(dayWeightCalculator.calculate(any(LocalDate.class), anyBoolean()))
                .thenReturn(DayDemandLevel.WEEKDAY);

        Map<LocalDateTime, ScoreResult> result = scoreService.calculateForecastScore(scoreTarget, REFERENCE_TIME);

        long distinctDates = result.keySet().stream()
                .map(LocalDateTime::toLocalDate)
                .distinct()
                .count();

        verify(holidayService, times((int) distinctDates)).isHoliday(any(LocalDate.class));
    }

    @Test
    @DisplayName("예보 조회에 실패하면 빈 결과를 돌려준다")
    void returnsEmptyWhenForecastFails() {
        ScoreTarget scoreTarget = createScoreTarget();

        when(forecastWeatherService.getForecastWeather(any(ScoreTarget.class), any(LocalDateTime.class)))
                .thenThrow(new KmaWeatherApiException("기상청 API 실패"));

        Map<LocalDateTime, ScoreResult> result = scoreService.calculateForecastScore(scoreTarget, REFERENCE_TIME);

        assertTrue(result.isEmpty());
    }

    private ForecastWeatherObservation createCurrentHourForecast() {
        return createForecast(REFERENCE_TIME.truncatedTo(ChronoUnit.HOURS));
    }

    private void assertCurrentAndFutureSlots(LocalDateTime referenceTime,
                                             LocalDateTime expectedCurrentHour) {
        ScoreTarget scoreTarget = createScoreTarget();
        List<ForecastWeatherObservation> forecasts = new ArrayList<>();
        for (int hour = 0; hour <= 6; hour++) {
            forecasts.add(createForecast(expectedCurrentHour.plusHours(hour)));
        }

        when(holidayService.isHoliday(any(LocalDate.class))).thenReturn(false);
        when(timeWeightCalculator.calculate(any(LocalTime.class)))
                .thenReturn(createNoImpactTime());
        when(dayWeightCalculator.calculate(any(LocalDate.class), anyBoolean()))
                .thenReturn(DayDemandLevel.WEEKDAY);
        when(forecastWeatherService.getForecastWeather(scoreTarget, referenceTime))
                .thenReturn(forecasts);
        when(forecastWeatherWeightCalculator.calculate(any(ForecastWeatherObservation.class)))
                .thenReturn(new WeatherScoreResult(0, List.of(), "날씨 영향 없음"));
        when(currentAirQualityService.getCurrentAirQuality(scoreTarget, referenceTime))
                .thenReturn(createAirQuality());
        when(airQualityCalculator.getWeight(any(CurrentAirQualityObservation.class)))
                .thenReturn(0);

        scoreService.calculateCurrentScore(scoreTarget, referenceTime);
        Map<LocalDateTime, ScoreResult> futureScores =
                scoreService.calculateForecastScore(scoreTarget, referenceTime);

        ArgumentCaptor<ForecastWeatherObservation> currentWeather =
                ArgumentCaptor.forClass(ForecastWeatherObservation.class);
        verify(forecastWeatherWeightCalculator).calculate(currentWeather.capture());
        assertEquals(expectedCurrentHour, currentWeather.getValue().getForecastAt());
        assertEquals(
                List.of(
                        expectedCurrentHour.plusHours(1),
                        expectedCurrentHour.plusHours(2),
                        expectedCurrentHour.plusHours(3),
                        expectedCurrentHour.plusHours(4),
                        expectedCurrentHour.plusHours(5)
                ),
                new ArrayList<>(futureScores.keySet())
        );
    }

    private ForecastWeatherObservation createForecast(LocalDateTime forecastAt) {
        // 날씨 보정이 0이 되는 값. 시간대·요일 점수만 남는다.
        return new ForecastWeatherObservation(forecastAt, 0, 0.0, 20.0, 50, 0.0);
    }

    private ScoreTarget createScoreTarget() {
        return new ScoreTarget(
                1L,
                "서울",
                "송파구",
                60,
                127,
                COMMERCIAL_AREA_CODE,
                BusinessType.CHICKEN
        );
    }

    /** 게스트 지역은 상권과 업종이 없다. */
    private ScoreTarget createGuestScoreTarget() {
        return new ScoreTarget(
                2L,
                "서울",
                "송파구",
                60,
                127,
                null,
                null
        );
    }


    private TimeDemandLevel createNoImpactTime() {
        return TimeDemandLevel.MEDIUM;
    }


    private CurrentAirQualityObservation createAirQuality() {
        return new CurrentAirQualityObservation(
                "서울",
                "송파구",
                "111123",
                "도시대기",
                LocalDateTime.of(2026, 1, 1, 0, 0),
                100,
                40,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
