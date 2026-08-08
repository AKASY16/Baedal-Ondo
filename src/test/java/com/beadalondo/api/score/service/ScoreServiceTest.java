package com.beadalondo.api.score.service;

import com.beadalondo.api.airquality.calculator.AirQualityCalculator;
import com.beadalondo.api.airquality.domain.CurrentAirQualityObservation;
import com.beadalondo.api.airquality.exception.AirKoreaApiException;
import com.beadalondo.api.airquality.service.CurrentAirQualityService;
import com.beadalondo.api.holiday.service.HolidayService;
import com.beadalondo.api.score.ScoreResult;
import com.beadalondo.api.score.calculator.DayWeightCalculator;
import com.beadalondo.api.score.dayweight.DayWeightProvider;
import com.beadalondo.api.store.domain.BusinessType;
import com.beadalondo.api.score.calculator.TimeWeightCalculator;
import com.beadalondo.api.score.calculator.WeightedScoreCalculator;
import com.beadalondo.api.score.dto.ScoreTarget;
import com.beadalondo.api.score.factory.ScoreMessageFactory;
import com.beadalondo.api.score.status.DayDemandLevel;
import com.beadalondo.api.score.status.TimeDemandLevel;
import com.beadalondo.api.weather.calculator.CurrentWeatherWeightCalculator;
import com.beadalondo.api.weather.domain.CurrentWeatherObservation;
import com.beadalondo.api.weather.domain.WeatherScoreResult;
import com.beadalondo.api.weather.exception.KmaWeatherApiException;
import com.beadalondo.api.weather.service.CurrentWeatherService;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
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

    @Spy
    private WeightedScoreCalculator weightedScoreCalculator = new WeightedScoreCalculator();

    @InjectMocks
    private ScoreService scoreService;

    @Test
    void airQualityScoreTest() {
        // given
        ScoreTarget scoreTarget = createScoreTarget();
        CurrentWeatherObservation weather = createNoImpactWeather();
        CurrentAirQualityObservation airQuality = createAirQuality();

        when(holidayService.isHoliday(any(LocalDate.class))).thenReturn(false);
        when(timeWeightCalculator.calculate(any(LocalTime.class)))
                .thenReturn(createNoImpactTime());
        when(dayWeightCalculator.calculate(any(LocalDate.class), anyBoolean()))
                .thenReturn(DayDemandLevel.WEEKDAY); // 0점
        when(currentWeatherService.getCurrentWeather(any(ScoreTarget.class)))
                .thenReturn(weather);
        when(currentWeatherWeightCalculator.calculate(any(CurrentWeatherObservation.class)))
                .thenReturn(new WeatherScoreResult(0, List.of(), "날씨 영향 없음"));
        when(currentAirQualityService.getCurrentAirQuality(any(ScoreTarget.class)))
                .thenReturn(airQuality);
        when(airQualityCalculator.getWeight(any(CurrentAirQualityObservation.class)))
                .thenReturn(5);

        // when
        ScoreResult result = scoreService.calculateCurrentScore(scoreTarget);

        // then
        assertEquals(56, result.getScore());
    }

    @Test
    void timeWeightScoreTest() {
        ScoreTarget scoreTarget = createScoreTarget();
        CurrentWeatherObservation weather = createNoImpactWeather();
        CurrentAirQualityObservation airQuality = createAirQuality();

        when(holidayService.isHoliday(any(LocalDate.class))).thenReturn(false);
        when(timeWeightCalculator.calculate(any(LocalTime.class)))
                .thenReturn(TimeDemandLevel.LOW); // +10점
        when(dayWeightCalculator.calculate(any(LocalDate.class), anyBoolean()))
                .thenReturn(DayDemandLevel.WEEKDAY); // 0점
        when(currentWeatherService.getCurrentWeather(any(ScoreTarget.class)))
                .thenReturn(weather);
        when(currentWeatherWeightCalculator.calculate(any(CurrentWeatherObservation.class)))
                .thenReturn(new WeatherScoreResult(0, List.of(), "날씨 영향 없음"));
        when(currentAirQualityService.getCurrentAirQuality(any(ScoreTarget.class)))
                .thenReturn(airQuality);
        when(airQualityCalculator.getWeight(any(CurrentAirQualityObservation.class)))
                .thenReturn(0);

        ScoreResult result = scoreService.calculateCurrentScore(scoreTarget);

        assertEquals(42, result.getScore());
    }

    @Test
    @DisplayName("상권 DayWeight가 요일 점수로 그대로 적용된다")
    void dayWeightScoreTest() {
        ScoreTarget scoreTarget = createScoreTarget();
        CurrentWeatherObservation weather = createNoImpactWeather();
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
                .thenReturn(weather);
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
        CurrentWeatherObservation weather = createNoImpactWeather();
        CurrentAirQualityObservation airQuality = createAirQuality();

        when(holidayService.isHoliday(any(LocalDate.class))).thenReturn(false);
        when(timeWeightCalculator.calculate(any(LocalTime.class)))
                .thenReturn(createNoImpactTime());
        when(dayWeightCalculator.calculate(any(LocalDate.class), anyBoolean()))
                .thenReturn(DayDemandLevel.WEEKEND);
        when(dayWeightProvider.findWeight(any(), any(), any()))
                .thenReturn(-6);
        when(currentWeatherService.getCurrentWeather(any(ScoreTarget.class)))
                .thenReturn(weather);
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
                LocalDate.now().getDayOfWeek());
    }

    @Test
    @DisplayName("게스트 지역은 상권과 업종 없이 조회되어 요일 점수가 0이 된다")
    void guestTargetHasNoMarketDayWeight() {
        ScoreTarget guestTarget = createGuestScoreTarget();
        stubCalmDependencies(DayDemandLevel.WEEKEND);

        ScoreResult result = scoreService.calculateCurrentScore(guestTarget);

        verify(dayWeightProvider).findWeight(null, null, LocalDate.now().getDayOfWeek());
        // 게스트는 주말이어도 기존 +8을 받지 않는다.
        assertEquals(50, result.getScore());
    }

    @Test
    void holidayScoreTest() {
        ScoreTarget scoreTarget = createScoreTarget();
        CurrentWeatherObservation weather = createNoImpactWeather();
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
                .thenReturn(weather);
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
        CurrentWeatherObservation weather = createLowImpactWeather();
        CurrentAirQualityObservation airQuality = createAirQuality();

        when(holidayService.isHoliday(any(LocalDate.class))).thenReturn(false);
        when(timeWeightCalculator.calculate(any(LocalTime.class)))
                .thenReturn(createNoImpactTime()); // 0점
        when(dayWeightCalculator.calculate(any(LocalDate.class), anyBoolean()))
                .thenReturn(DayDemandLevel.WEEKDAY); // 0점
        when(currentWeatherService.getCurrentWeather(any(ScoreTarget.class)))
                .thenReturn(weather);
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
        CurrentWeatherObservation weather = createNoImpactWeather();
        CurrentAirQualityObservation airQuality = createAirQuality();

        when(holidayService.isHoliday(any(LocalDate.class))).thenReturn(false);
        when(timeWeightCalculator.calculate(any(LocalTime.class)))
                .thenReturn(TimeDemandLevel.VERY_HIGH);
        when(dayWeightCalculator.calculate(any(LocalDate.class), anyBoolean()))
                .thenReturn(DayDemandLevel.WEEKEND);
        when(currentWeatherService.getCurrentWeather(any(ScoreTarget.class)))
                .thenReturn(weather);
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
        CurrentWeatherObservation weather = createNoImpactWeather();

        when(holidayService.isHoliday(any(LocalDate.class))).thenReturn(false);
        when(timeWeightCalculator.calculate(any(LocalTime.class)))
                .thenReturn(createNoImpactTime()); // 0점
        when(dayWeightCalculator.calculate(any(LocalDate.class), anyBoolean()))
                .thenReturn(DayDemandLevel.WEEKDAY); // 0점
        when(currentWeatherService.getCurrentWeather(any(ScoreTarget.class)))
                .thenReturn(weather);
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
        CurrentWeatherObservation weather = createNoImpactWeather();
        CurrentAirQualityObservation airQuality = createAirQuality();

        when(holidayService.isHoliday(any(LocalDate.class)))
                .thenThrow(new RuntimeException("공휴일 조회 테스트 예외"));
        when(timeWeightCalculator.calculate(any(LocalTime.class)))
                .thenReturn(createNoImpactTime()); // 0점
        when(dayWeightCalculator.calculate(any(LocalDate.class), eq(false)))
                .thenReturn(DayDemandLevel.WEEKDAY); // 0점
        when(currentWeatherService.getCurrentWeather(any(ScoreTarget.class)))
                .thenReturn(weather);
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

    private CurrentWeatherObservation createNoImpactWeather() {
        return new CurrentWeatherObservation(0, 0, 20, 50, 0);
    }

    private CurrentWeatherObservation createLowImpactWeather() {
        //풍속만 점수에 영향을 끼치므로, +4점
        return new CurrentWeatherObservation(0, 0, 20, 50, 17);
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
                0.1,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
