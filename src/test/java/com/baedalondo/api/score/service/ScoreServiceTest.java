package com.baedalondo.api.score.service;

import com.baedalondo.api.common.ServiceTime;
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
import com.baedalondo.api.weather.calculator.CurrentWeatherWeightCalculator;
import com.baedalondo.api.weather.calculator.ForecastWeatherWeightCalculator;
import com.baedalondo.api.weather.calculator.WeatherWeightCalculator;
import com.baedalondo.api.weather.domain.CurrentWeatherObservation;
import com.baedalondo.api.weather.domain.ForecastWeatherObservation;
import com.baedalondo.api.weather.domain.WeatherScoreResult;
import com.baedalondo.api.weather.exception.KmaWeatherApiException;
import com.baedalondo.api.weather.service.CurrentWeatherService;
import com.baedalondo.api.weather.service.ForecastWeatherService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
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

    @Mock
    private TimeWeightCalculator timeWeightCalculator;

    @Mock
    private DayWeightCalculator dayWeightCalculator;

    @Mock
    private DayWeightProvider dayWeightProvider;

    @Mock
    private TimeWeightProvider timeWeightProvider;

    @Mock
    private CurrentWeatherWeightCalculator currentWeatherWeightCalculator;

    @Mock
    private CurrentWeatherService currentWeatherService;

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

    @Spy
    private ForecastWeatherWeightCalculator forecastWeatherWeightCalculator =
            new ForecastWeatherWeightCalculator(new WeatherWeightCalculator());

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
        when(currentWeatherService.getCurrentWeather(any(ScoreTarget.class)))
                .thenReturn(createNoImpactWeather());
        when(currentWeatherWeightCalculator.calculate(any(CurrentWeatherObservation.class)))
                .thenReturn(new WeatherScoreResult(0, List.of(), "날씨 영향 없음"));
        when(currentAirQualityService.getCurrentAirQuality(any(ScoreTarget.class)))
                .thenReturn(airQuality);
        when(airQualityCalculator.getWeight(any(CurrentAirQualityObservation.class)))
                .thenReturn(5);

        // when
        ScoreResult result = scoreService.calculateCurrentScore(scoreTarget);

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
        when(currentWeatherService.getCurrentWeather(any(ScoreTarget.class)))
                .thenReturn(createNoImpactWeather());
        when(currentWeatherWeightCalculator.calculate(any(CurrentWeatherObservation.class)))
                .thenReturn(new WeatherScoreResult(0, List.of(), "날씨 영향 없음"));
        when(currentAirQualityService.getCurrentAirQuality(any(ScoreTarget.class)))
                .thenReturn(airQuality);
        when(airQualityCalculator.getWeight(any(CurrentAirQualityObservation.class)))
                .thenReturn(0);

        ScoreResult result = scoreService.calculateCurrentScore(scoreTarget);

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
        when(currentWeatherService.getCurrentWeather(any(ScoreTarget.class)))
                .thenReturn(createNoImpactWeather());
        when(currentWeatherWeightCalculator.calculate(any(CurrentWeatherObservation.class)))
                .thenReturn(new WeatherScoreResult(0, List.of(), "날씨 영향 없음"));
        when(currentAirQualityService.getCurrentAirQuality(any(ScoreTarget.class)))
                .thenReturn(airQuality);
        when(airQualityCalculator.getWeight(any(CurrentAirQualityObservation.class)))
                .thenReturn(0);

        ScoreResult result = scoreService.calculateCurrentScore(scoreTarget);

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
        when(currentWeatherService.getCurrentWeather(any(ScoreTarget.class)))
                .thenReturn(createNoImpactWeather());
        when(currentWeatherWeightCalculator.calculate(any(CurrentWeatherObservation.class)))
                .thenReturn(new WeatherScoreResult(0, List.of(), "날씨 영향 없음"));
        when(currentAirQualityService.getCurrentAirQuality(any(ScoreTarget.class)))
                .thenReturn(airQuality);
        when(airQualityCalculator.getWeight(any(CurrentAirQualityObservation.class)))
                .thenReturn(0);

        ScoreResult result = scoreService.calculateCurrentScore(scoreTarget);

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
        when(currentWeatherService.getCurrentWeather(any(ScoreTarget.class)))
                .thenReturn(createNoImpactWeather());
        when(currentWeatherWeightCalculator.calculate(any(CurrentWeatherObservation.class)))
                .thenReturn(new WeatherScoreResult(0, List.of(), "날씨 영향 없음"));
        when(currentAirQualityService.getCurrentAirQuality(any(ScoreTarget.class)))
                .thenReturn(airQuality);
        when(airQualityCalculator.getWeight(any(CurrentAirQualityObservation.class)))
                .thenReturn(0);

        ScoreResult result = scoreService.calculateCurrentScore(scoreTarget);

        assertEquals(44, result.getScore());
    }

    @Test
    @DisplayName("Store의 상권코드와 업종, 오늘 요일로 DayWeight를 조회한다")
    void passesStoreKeysToDayWeightProvider() {
        ScoreTarget scoreTarget = createScoreTarget();
        stubCalmDependencies(DayDemandLevel.WEEKDAY);

        scoreService.calculateCurrentScore(scoreTarget);

        verify(dayWeightProvider).findWeight(
                COMMERCIAL_AREA_CODE,
                BusinessType.CHICKEN,
                ServiceTime.today().getDayOfWeek());
    }

    @Test
    @DisplayName("게스트 지역은 상권과 업종 없이 조회되어 요일 점수가 0이 된다")
    void guestTargetHasNoMarketDayWeight() {
        ScoreTarget guestTarget = createGuestScoreTarget();
        stubCalmDependencies(DayDemandLevel.WEEKEND);

        ScoreResult result = scoreService.calculateCurrentScore(guestTarget);

        verify(dayWeightProvider).findWeight(null, null, ServiceTime.today().getDayOfWeek());
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
        when(currentWeatherService.getCurrentWeather(any(ScoreTarget.class)))
                .thenReturn(createNoImpactWeather());
        when(currentWeatherWeightCalculator.calculate(any(CurrentWeatherObservation.class)))
                .thenReturn(new WeatherScoreResult(0, List.of(), "날씨 영향 없음"));
        when(currentAirQualityService.getCurrentAirQuality(any(ScoreTarget.class)))
                .thenReturn(airQuality);
        when(airQualityCalculator.getWeight(any(CurrentAirQualityObservation.class)))
                .thenReturn(0);

        ScoreResult result = scoreService.calculateCurrentScore(scoreTarget);

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
        when(currentWeatherService.getCurrentWeather(any(ScoreTarget.class)))
                .thenReturn(createNoImpactWeather());
        when(currentWeatherWeightCalculator.calculate(any(CurrentWeatherObservation.class)))
                .thenReturn(new WeatherScoreResult(4, List.of("풍속"), "풍속")); // +4점
        when(currentAirQualityService.getCurrentAirQuality(any(ScoreTarget.class)))
                .thenReturn(airQuality);
        when(airQualityCalculator.getWeight(any(CurrentAirQualityObservation.class)))
                .thenReturn(0);

        ScoreResult result = scoreService.calculateCurrentScore(scoreTarget);

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
        when(currentWeatherService.getCurrentWeather(any(ScoreTarget.class)))
                .thenReturn(createNoImpactWeather());
        when(currentWeatherWeightCalculator.calculate(any(CurrentWeatherObservation.class)))
                .thenReturn(new WeatherScoreResult(100, List.of("100점 초과 테스트"), "100점 초과 테스트")); // +100점
        when(currentAirQualityService.getCurrentAirQuality(any(ScoreTarget.class)))
                .thenReturn(airQuality);
        when(airQualityCalculator.getWeight(any(CurrentAirQualityObservation.class)))
                .thenReturn(7);

        ScoreResult result = scoreService.calculateCurrentScore(scoreTarget);

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
        when(currentWeatherService.getCurrentWeather(any(ScoreTarget.class)))
                .thenThrow(new KmaWeatherApiException("날씨 API 테스트 예외"));
        when(currentAirQualityService.getCurrentAirQuality(any(ScoreTarget.class)))
                .thenReturn(airQuality);
        when(airQualityCalculator.getWeight(any(CurrentAirQualityObservation.class)))
                .thenReturn(0);

        ScoreResult result = scoreService.calculateCurrentScore(scoreTarget);

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
        when(currentWeatherService.getCurrentWeather(any(ScoreTarget.class)))
                .thenReturn(createNoImpactWeather());
        when(currentWeatherWeightCalculator.calculate(any(CurrentWeatherObservation.class)))
                .thenReturn(new WeatherScoreResult(0, List.of(), "날씨 영향 없음"));
        when(currentAirQualityService.getCurrentAirQuality(any(ScoreTarget.class)))
                .thenThrow(new AirKoreaApiException("공기질 API 테스트 예외"));

        ScoreResult result = scoreService.calculateCurrentScore(scoreTarget);

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
        when(currentWeatherService.getCurrentWeather(any(ScoreTarget.class)))
                .thenReturn(createNoImpactWeather());
        when(currentWeatherWeightCalculator.calculate(any(CurrentWeatherObservation.class)))
                .thenReturn(new WeatherScoreResult(0, List.of(), "날씨 영향 없음"));
        when(currentAirQualityService.getCurrentAirQuality(any(ScoreTarget.class)))
                .thenReturn(airQuality);
        when(airQualityCalculator.getWeight(any(CurrentAirQualityObservation.class)))
                .thenReturn(0);

        ScoreResult result = scoreService.calculateCurrentScore(scoreTarget);

        assertEquals(50, result.getScore());
    }


    /** 시간/날씨/대기질 영향이 전혀 없는 상태로 만들어 요일 점수만 관찰한다. */
    private void stubCalmDependencies(DayDemandLevel dayDemandLevel) {
        when(holidayService.isHoliday(any(LocalDate.class))).thenReturn(false);
        when(timeWeightCalculator.calculate(any(LocalTime.class)))
                .thenReturn(createNoImpactTime());
        when(dayWeightCalculator.calculate(any(LocalDate.class), anyBoolean()))
                .thenReturn(dayDemandLevel);
        when(currentWeatherService.getCurrentWeather(any(ScoreTarget.class)))
                .thenReturn(createNoImpactWeather());
        when(currentWeatherWeightCalculator.calculate(any(CurrentWeatherObservation.class)))
                .thenReturn(new WeatherScoreResult(0, List.of(), "날씨 영향 없음"));
        when(currentAirQualityService.getCurrentAirQuality(any(ScoreTarget.class)))
                .thenReturn(createAirQuality());
        when(airQualityCalculator.getWeight(any(CurrentAirQualityObservation.class)))
                .thenReturn(0);
    }

    @Test
    @DisplayName("이미 지난 예보는 계산에서 제외한다")
    void excludesPastForecasts() {
        ScoreTarget scoreTarget = createScoreTarget();
        LocalDateTime base = ServiceTime.now().truncatedTo(ChronoUnit.HOURS);

        when(forecastWeatherService.getForecastWeather(any(ScoreTarget.class)))
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

        Map<LocalDateTime, ScoreResult> result = scoreService.calculateForecastScore(scoreTarget);

        assertEquals(2, result.size());
        assertTrue(result.containsKey(base.plusHours(2)));
        assertTrue(result.containsKey(base.plusHours(3)));
    }

    @Test
    @DisplayName("예보가 많아도 6시간까지만 계산한다")
    void limitsToSixHours() {
        ScoreTarget scoreTarget = createScoreTarget();
        LocalDateTime base = ServiceTime.now().truncatedTo(ChronoUnit.HOURS);

        List<ForecastWeatherObservation> forecasts = new ArrayList<>();
        for (int hour = 1; hour <= 9; hour++) {
            forecasts.add(createForecast(base.plusHours(hour)));
        }

        when(forecastWeatherService.getForecastWeather(any(ScoreTarget.class)))
                .thenReturn(forecasts);
        when(holidayService.isHoliday(any(LocalDate.class))).thenReturn(false);
        when(timeWeightCalculator.calculate(any(LocalTime.class)))
                .thenReturn(createNoImpactTime());
        when(dayWeightCalculator.calculate(any(LocalDate.class), anyBoolean()))
                .thenReturn(DayDemandLevel.WEEKDAY);

        Map<LocalDateTime, ScoreResult> result = scoreService.calculateForecastScore(scoreTarget);

        assertEquals(6, result.size());
    }

    @Test
    @DisplayName("공휴일 조회는 예보 건수가 아니라 날짜 수만큼만 한다")
    void looksUpHolidayOncePerDate() {
        // 공휴일 조회는 비공휴일마다 외부 API를 호출하는 구조라
        // 예보 6건에 6번 부르면 요청당 왕복이 6배가 된다.
        ScoreTarget scoreTarget = createScoreTarget();
        LocalDateTime base = ServiceTime.now().truncatedTo(ChronoUnit.HOURS);

        List<ForecastWeatherObservation> forecasts = new ArrayList<>();
        for (int hour = 1; hour <= 6; hour++) {
            forecasts.add(createForecast(base.plusHours(hour)));
        }

        when(forecastWeatherService.getForecastWeather(any(ScoreTarget.class)))
                .thenReturn(forecasts);
        when(holidayService.isHoliday(any(LocalDate.class))).thenReturn(false);
        when(timeWeightCalculator.calculate(any(LocalTime.class)))
                .thenReturn(createNoImpactTime());
        when(dayWeightCalculator.calculate(any(LocalDate.class), anyBoolean()))
                .thenReturn(DayDemandLevel.WEEKDAY);

        Map<LocalDateTime, ScoreResult> result = scoreService.calculateForecastScore(scoreTarget);

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

        when(forecastWeatherService.getForecastWeather(any(ScoreTarget.class)))
                .thenThrow(new KmaWeatherApiException("기상청 API 실패"));

        Map<LocalDateTime, ScoreResult> result = scoreService.calculateForecastScore(scoreTarget);

        assertTrue(result.isEmpty());
    }

    private CurrentWeatherObservation createNoImpactWeather() {
        return new CurrentWeatherObservation(0, 0, 20, 50, 0);
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
