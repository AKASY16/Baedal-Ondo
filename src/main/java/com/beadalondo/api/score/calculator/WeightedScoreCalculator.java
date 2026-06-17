package com.beadalondo.api.score.calculator;

import com.beadalondo.api.score.ScoreBreakdown;
import com.beadalondo.api.score.status.DayDemandLevel;
import com.beadalondo.api.score.status.TimeDemandLevel;
import com.beadalondo.api.weather.domain.CurrentWeatherObservation;
import com.beadalondo.api.weather.domain.WeatherScoreResult;
import org.springframework.stereotype.Component;

@Component
public class WeightedScoreCalculator {

    private static final int BASE_SCORE = 50;
    private static final int RAW_WEATHER_MAX = 17;
    private static final int WEIGHTED_WEATHER_MAX = 20;
    private static final int RAW_AIR_QUALITY_MAX = 7;
    private static final int WEIGHTED_AIR_QUALITY_MAX = 8;
    private static final int INTERACTION_MAX = 10;

    public ScoreBreakdown calculate(TimeDemandLevel timeDemandLevel,
                                    DayDemandLevel dayDemandLevel,
                                    WeatherScoreResult weatherScoreResult,
                                    CurrentWeatherObservation weather,
                                    int rawAirQualityScore) {
        int timeScore = calculateTimeScore(timeDemandLevel);
        int dayScore = calculateDayScore(dayDemandLevel);
        int weatherScore = normalize(
                weatherScoreResult == null ? 0 : weatherScoreResult.getWeatherScore(),
                RAW_WEATHER_MAX,
                WEIGHTED_WEATHER_MAX
        );
        int airQualityScore = normalize(rawAirQualityScore, RAW_AIR_QUALITY_MAX, WEIGHTED_AIR_QUALITY_MAX);
        int interactionScore = calculateInteractionScore(timeDemandLevel, dayDemandLevel, weather);

        int uncappedScore = BASE_SCORE
                + timeScore
                + dayScore
                + weatherScore
                + airQualityScore
                + interactionScore;
        int score = capScore(applyTimeLevelCap(uncappedScore, timeDemandLevel));

        return new ScoreBreakdown(
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
            case VERY_HIGH -> 24;
            case HIGH -> 14;
            case MEDIUM -> 0;
            case LOW -> -8;
            case CLOSED -> -18;
        };
    }

    private int calculateDayScore(DayDemandLevel level) {
        if (level == null) {
            return 0;
        }

        return switch (level) {
            case FRIDAY -> 6;
            case WEEKEND, HOLIDAY -> 8;
            case WEEKDAY -> 0;
        };
    }

    private int calculateInteractionScore(TimeDemandLevel timeLevel,
                                          DayDemandLevel dayLevel,
                                          CurrentWeatherObservation weather) {
        int score = 0;

        if (isEveningPeak(timeLevel)) {
            score += switch (dayLevel) {
                case FRIDAY -> 5;
                case WEEKEND -> 7;
                case HOLIDAY -> 4;
                default -> 0;
            };
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

    private boolean isEveningPeak(TimeDemandLevel timeLevel) {
        return timeLevel == TimeDemandLevel.VERY_HIGH || timeLevel == TimeDemandLevel.HIGH;
    }

    private boolean isRainy(CurrentWeatherObservation weather) {
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

    private int applyTimeLevelCap(int score, TimeDemandLevel timeDemandLevel) {
        if (timeDemandLevel == null) {
            return score;
        }

        return switch (timeDemandLevel) {
            case CLOSED -> Math.min(score, 39);
            case LOW -> Math.min(score, 59);
            case MEDIUM -> Math.min(score, 79);
            case HIGH, VERY_HIGH -> score;
        };
    }

    private int capScore(int score) {
        return Math.max(0, Math.min(100, score));
    }
}
