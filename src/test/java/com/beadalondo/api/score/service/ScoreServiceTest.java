package com.beadalondo.api.score.service;

import com.beadalondo.api.airquality.calculator.AirQualityCalculator;
import com.beadalondo.api.airquality.domain.CurrentAirQualityObservation;
import com.beadalondo.api.airquality.exception.AirKoreaApiException;
import com.beadalondo.api.airquality.service.CurrentAirQualityService;
import com.beadalondo.api.holiday.service.HolidayService;
import com.beadalondo.api.score.ScoreResult;
import com.beadalondo.api.score.calculator.DayWeightCalculator;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScoreServiceTest {

    @Mock
    private TimeWeightCalculator timeWeightCalculator;

    @Mock
    private DayWeightCalculator dayWeightCalculator;

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
    void dayWeightScoreTest() {
        ScoreTarget scoreTarget = createScoreTarget();
        CurrentWeatherObservation weather = createNoImpactWeather();
        CurrentAirQualityObservation airQuality = createAirQuality();

        when(holidayService.isHoliday(any(LocalDate.class))).thenReturn(false);
        when(timeWeightCalculator.calculate(any(LocalTime.class)))
                .thenReturn(createNoImpactTime()); // 0점
        when(dayWeightCalculator.calculate(any(LocalDate.class), anyBoolean()))
                .thenReturn(DayDemandLevel.WEEKEND); // +10점
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
    void holidayScoreTest() {
        ScoreTarget scoreTarget = createScoreTarget();
        CurrentWeatherObservation weather = createNoImpactWeather();
        CurrentAirQualityObservation airQuality = createAirQuality();

        when(holidayService.isHoliday(any(LocalDate.class))).thenReturn(true);
        when(timeWeightCalculator.calculate(any(LocalTime.class)))
                .thenReturn(createNoImpactTime()); // 0점
        when(dayWeightCalculator.calculate(any(LocalDate.class), eq(true)))
                .thenReturn(DayDemandLevel.HOLIDAY); // +10점
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







    private ScoreTarget createScoreTarget() {
        return new ScoreTarget(
                1L,
                "서울",
                "송파구",
                60,
                127
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
