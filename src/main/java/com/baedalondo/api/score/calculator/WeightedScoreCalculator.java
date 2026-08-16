package com.baedalondo.api.score.calculator;

import com.baedalondo.api.score.ScoreCalculationResult;
import com.baedalondo.api.score.status.DayDemandLevel;
import com.baedalondo.api.score.status.TimeDemandLevel;
import com.baedalondo.api.weather.domain.WeatherMeasurement;
import com.baedalondo.api.weather.domain.WeatherScoreResult;
import org.springframework.stereotype.Component;

@Component
public class WeightedScoreCalculator {

    private static final int BASE_SCORE = 50;
    private static final int HOLIDAY_SCORE = 8;
    private static final int RAW_WEATHER_MAX = 17;
    private static final int WEIGHTED_WEATHER_MAX = 20;
    private static final int RAW_AIR_QUALITY_MAX = 5;
    private static final int WEIGHTED_AIR_QUALITY_MAX = 8;
    private static final int INTERACTION_MAX = 10;
    private static final int DAY_PEAK_INTERACTION_MAX = 3;
    private static final int HOLIDAY_PEAK_INTERACTION = 4;

    /**
     marketDayWeight는 상권 x 업종 x 요일 기준으로 미리 계산된 값(-6 ~ +6)이다.
     공휴일이 아닌 날에는 이 값이 기존 요일 점수를 대체한다. 함께 더하지 않는다.
     */
    public ScoreCalculationResult calculate(TimeDemandLevel timeDemandLevel,
                                            DayDemandLevel dayDemandLevel,
                                            int marketDayWeight,
                                            WeatherScoreResult weatherScoreResult,
                                            WeatherMeasurement weather,
                                            int rawAirQualityScore) {
        int timeScore = calculateTimeScore(timeDemandLevel);
        int dayScore = calculateDayScore(dayDemandLevel, marketDayWeight);
        int weatherScore = normalize(
                weatherScoreResult == null ? 0 : weatherScoreResult.getWeatherScore(),
                RAW_WEATHER_MAX,
                WEIGHTED_WEATHER_MAX
        );
        int airQualityScore = normalize(rawAirQualityScore, RAW_AIR_QUALITY_MAX, WEIGHTED_AIR_QUALITY_MAX);
        int interactionScore = calculateInteractionScore(
                timeDemandLevel, dayDemandLevel, marketDayWeight, weather);

        int uncappedScore = BASE_SCORE
                + timeScore
                + dayScore
                + weatherScore
                + airQualityScore
                + interactionScore;
        int score = capScore(uncappedScore);

        return new ScoreCalculationResult(
                score,
                timeScore,
                dayScore,
                weatherScore,
                airQualityScore,
                interactionScore
        );
    }

    private int calculateTimeScore(TimeDemandLevel level) {
        if (level == null) {
            return 0;
        }

        return switch (level) {
            case VERY_HIGH -> 14;
            case HIGH -> 8;
            case MEDIUM -> 0;
            case LOW -> -6;
            case CLOSED -> -12;
        };
    }

    /**
     공휴일은 전처리 데이터가 따로 분리하지 못하는 효과라 기존 고정 점수를 유지한다.
     그 외의 날은 상권 x 업종 x 요일 가중치가 요일 점수를 그대로 대체한다.
     평일, 금요일, 주말이라는 이유로 붙던 고정 점수는 더 이상 사용하지 않는다.
     */
    private int calculateDayScore(DayDemandLevel level, int marketDayWeight) {
        if (level == DayDemandLevel.HOLIDAY) {
            return HOLIDAY_SCORE;
        }

        return marketDayWeight;
    }

    private int calculateInteractionScore(TimeDemandLevel timeLevel,
                                          DayDemandLevel dayLevel,
                                          int marketDayWeight,
                                          WeatherMeasurement weather) {
        int score = 0;

        if (isEveningPeak(timeLevel)) {
            if (dayLevel == DayDemandLevel.HOLIDAY) {
                score += HOLIDAY_PEAK_INTERACTION;
            } else {
                score += calculateDayPeakInteraction(marketDayWeight);
            }
        }

        if (isRainy(weather)) {
            if (timeLevel == TimeDemandLevel.VERY_HIGH) {
                score += 5;
            } else if (timeLevel == TimeDemandLevel.HIGH) {
                score += 3;
            }

            if (dayLevel == DayDemandLevel.HOLIDAY) {
                score += 4;
            }
        }

        return Math.min(score, INTERACTION_MAX);
    }

    /**
     수요가 강한 요일과 피크 시간대가 겹칠 때 상승 신호를 조금 더 분명히 보여주기 위한
     제한적인 product heuristic이다. 요일 x 시간대 교차 데이터로 추정한 통계 효과가 아니다.

     그래서 다음을 지킨다.
     - 음수 DayWeight에는 적용하지 않는다.
     - DayWeight의 방향을 뒤집지 않는다.
     - 요일 이름(금요일, 주말)을 조건으로 쓰지 않는다.
     - 최대 +3으로 제한한다.

     공휴일에는 적용하지 않는다. 공휴일 dayScore는 marketDayWeight를 의도적으로
     무시하고 고정 +8을 쓰므로, interaction에서만 다시 쓰면 일관되지 않는다.
     공휴일의 상승 효과는 dayScore +8, 저녁 피크 +4, 비 +4로 이미 반영된다.
     */
    private int calculateDayPeakInteraction(int marketDayWeight) {
        if (marketDayWeight <= 0) {
            return 0;
        }

        // 양수에서 (w + 1) / 2 는 ceil(w / 2)와 같다. +1,+2 -> 1 / +3,+4 -> 2 / +5,+6 -> 3
        return Math.min((marketDayWeight + 1) / 2, DAY_PEAK_INTERACTION_MAX);
    }

    private boolean isEveningPeak(TimeDemandLevel timeLevel) {
        return timeLevel == TimeDemandLevel.VERY_HIGH || timeLevel == TimeDemandLevel.HIGH;
    }

    // 실황과 예보를 모두 받는다. 미래 시각 점수를 계산할 때 예보 관측값이 그대로 들어온다.
    private boolean isRainy(WeatherMeasurement weather) {
        if (weather == null) {
            return false;
        }

        return weather.getRainfall() > 0 || weather.getPrecipitationType() != 0;
    }

    private int normalize(int rawScore, int rawMax, int weightedMax) {
        if (rawScore <= 0) {
            return 0;
        }

        int cappedRawScore = Math.min(rawScore, rawMax);
        return (int) Math.round((double) cappedRawScore * weightedMax / rawMax);
    }

    private int capScore(int score) {
        return Math.max(0, Math.min(100, score));
    }
}
