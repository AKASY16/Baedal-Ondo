package com.baedalondo.api;

import com.baedalondo.api.common.ServiceTime;
import com.baedalondo.api.airquality.domain.CurrentAirQualityObservation;
import com.baedalondo.api.score.factory.ScoreMessageFactory;
import com.baedalondo.api.score.status.DayDemandLevel;
import com.baedalondo.api.score.status.TimeDemandLevel;
import com.baedalondo.api.weather.calculator.WeatherWeightCalculator;
import com.baedalondo.api.weather.domain.ForecastWeatherObservation;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserFacingFactorDescriptionTest {

    private static final LocalDateTime WEATHER_AT = LocalDateTime.of(2026, 8, 16, 20, 0);

    private final WeatherWeightCalculator weatherCalculator = new WeatherWeightCalculator();
    private final ScoreMessageFactory messageFactory = new ScoreMessageFactory();

    @Test
    void describesWeatherInEverydayLanguage() {
        ForecastWeatherObservation calm = new ForecastWeatherObservation(WEATHER_AT, 0, 0, 20, 50, 1);
        ForecastWeatherObservation rainyAndWindy = new ForecastWeatherObservation(WEATHER_AT, 0, 3, 20, 50, 9);
        ForecastWeatherObservation snowyAndCold = new ForecastWeatherObservation(WEATHER_AT, 3, 0, 0, 50, 1);

        assertEquals("외출에 큰 불편이 없는 날씨", weatherCalculator.calculate(calm).getDescription());
        assertEquals("비와 강한 바람이 겹친 날씨", weatherCalculator.calculate(rainyAndWindy).getDescription());
        assertEquals("눈과 낮은 기온이 겹친 날씨", weatherCalculator.calculate(snowyAndCold).getDescription());
    }

    @Test
    void adjustsTemperatureWordingToImpactStrength() {
        ForecastWeatherObservation slightlyHot = new ForecastWeatherObservation(WEATHER_AT, 0, 0, 26, 50, 1);
        ForecastWeatherObservation hot = new ForecastWeatherObservation(WEATHER_AT, 0, 0, 29, 50, 1);
        ForecastWeatherObservation veryHot = new ForecastWeatherObservation(WEATHER_AT, 0, 0, 32, 50, 1);

        assertEquals("기온이 조금 높은 편", weatherCalculator.calculate(slightlyHot).getDescription());
        assertEquals("높은 기온으로 외출이 다소 불편한 날씨", weatherCalculator.calculate(hot).getDescription());
        assertEquals("매우 높은 기온으로 외출이 불편한 날씨", weatherCalculator.calculate(veryHot).getDescription());
    }

    @Test
    void describesAirQualityByOutdoorDiscomfort() {
        CurrentAirQualityObservation airQuality = createAirQuality();

        assertEquals(
                "외출이 다소 불편한 대기질",
                messageFactory.createAirQualityDescription(airQuality, 2)
        );
        assertEquals(
                "외출에 불편한 대기질",
                messageFactory.createAirQualityDescription(airQuality, 4)
        );
        assertEquals(
                "외출에 큰 불편이 없는 대기질",
                messageFactory.createAirQualityDescription(airQuality, 0)
        );
    }

    @Test
    void describesLocalBusinessPatternWithTheActualDay() {
        assertEquals(
                "이 지역의 치킨 업종은 목요일 흐름이 비교적 활발한 편",
                messageFactory.createLocalPatternDescription(
                        DayDemandLevel.WEEKDAY,
                        4,
                        "치킨",
                        DayOfWeek.THURSDAY
                )
        );
        assertEquals(
                "이 지역의 치킨 업종은 일요일 흐름이 비교적 한산한 편",
                messageFactory.createLocalPatternDescription(
                        DayDemandLevel.WEEKEND,
                        -3,
                        "치킨",
                        DayOfWeek.SUNDAY
                )
        );
        assertEquals(
                "일요일 주문 흐름은 평소와 비슷한 편",
                messageFactory.createLocalPatternDescription(
                        DayDemandLevel.WEEKEND,
                        0,
                        null,
                        DayOfWeek.SUNDAY
                )
        );
    }

    @Test
    void timeDescriptionsMatchTheCurrentTimeBand() {
        assertEquals(
                "저녁 주문 흐름이 특히 강한 시간대",
                messageFactory.createTimeDescription(TimeDemandLevel.VERY_HIGH, LocalTime.of(18, 0))
        );
        assertEquals(
                "늦은 시간에도 주문 흐름이 강한 시간대",
                messageFactory.createTimeDescription(TimeDemandLevel.HIGH, LocalTime.of(22, 30))
        );
        assertEquals(
                "늦은 시간, 주문 흐름이 잦아드는 구간",
                messageFactory.createTimeDescription(TimeDemandLevel.LOW, LocalTime.of(22, 30))
        );
    }

    private CurrentAirQualityObservation createAirQuality() {
        return new CurrentAirQualityObservation(
                "서울",
                "송파구",
                "station",
                "임시대기",
                ServiceTime.now(),
                90,
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
