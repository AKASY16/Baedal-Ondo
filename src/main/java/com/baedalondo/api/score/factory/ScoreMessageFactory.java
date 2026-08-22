package com.baedalondo.api.score.factory;

import com.baedalondo.api.airquality.domain.CurrentAirQualityObservation;
import com.baedalondo.api.score.status.DayDemandLevel;
import com.baedalondo.api.score.status.ScoreStatusLevel;
import com.baedalondo.api.score.status.TimeDemandLevel;
import com.baedalondo.api.score.timeweight.TimeBand;
import com.baedalondo.api.weather.domain.WeatherScoreResult;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

@Component
public class ScoreMessageFactory {

    public String createWeatherFactor(WeatherScoreResult weatherScoreResult) {
        if (weatherScoreResult.getWeatherScore() <= 0) {
            return "•";
        }

        return "↑";
    }

    /** 실제 적용된 요일 점수의 방향을 표시한다. */
    public String createDayFactor(int appliedDayScore) {
        if (appliedDayScore > 0) {
            return "↑";
        }

        if (appliedDayScore < 0) {
            return "↓";
        }

        return "•";
    }

    public String createAirQualityFactor(int airQualityScore) {
        if (airQualityScore <= 0) {
            return "•";
        }

        return "↑";
    }

    public String createAirQualityDescription(CurrentAirQualityObservation airQuality,
                                               int rawAirQualityScore) {
        if (airQuality == null) {
            return "대기질 정보를 확인하지 못했어요";
        }

        if (rawAirQualityScore >= 4) {
            return "외출에 불편한 대기질";
        }

        if (rawAirQualityScore >= 2) {
            return "외출이 다소 불편한 대기질";
        }

        if (rawAirQualityScore == 1) {
            return "외출이 조금 꺼려지는 대기질";
        }

        return "외출에 큰 불편이 없는 대기질";
    }

    public String createTimeDescription(TimeDemandLevel timeDemandLevel, LocalTime currentTime) {
        TimeBand timeBand = TimeBand.from(currentTime);

        return switch (timeDemandLevel) {
            case VERY_HIGH -> switch (timeBand) {
                case TIME_00_06 -> "심야에도 주문 흐름이 특히 강한 시간대";
                case TIME_06_11 -> "오전 주문 흐름이 특히 강한 시간대";
                case TIME_11_14 -> "점심 주문 흐름이 특히 강한 시간대";
                case TIME_14_17 -> "오후 주문 흐름이 특히 강한 시간대";
                case TIME_17_21 -> "저녁 주문 흐름이 특히 강한 시간대";
                case TIME_21_24 -> "늦은 시간에도 주문 흐름이 특히 강한 시간대";
            };
            case HIGH -> switch (timeBand) {
                case TIME_00_06 -> "심야에도 주문 흐름이 강한 시간대";
                case TIME_06_11 -> "오전 주문 흐름이 강한 시간대";
                case TIME_11_14 -> "점심 주문 흐름이 강한 시간대";
                case TIME_14_17 -> "오후 주문 흐름이 강한 시간대";
                case TIME_17_21 -> "저녁 주문 흐름이 강한 시간대";
                case TIME_21_24 -> "늦은 시간에도 주문 흐름이 강한 시간대";
            };
            case MEDIUM -> switch (timeBand) {
                case TIME_00_06 -> "심야 주문 흐름이 평소와 비슷한 시간대";
                case TIME_06_11 -> "오전 주문 흐름이 평소와 비슷한 시간대";
                case TIME_11_14 -> "점심 주문 흐름이 평소와 비슷한 시간대";
                case TIME_14_17 -> "오후 주문 흐름이 평소와 비슷한 시간대";
                case TIME_17_21 -> "저녁 주문 흐름이 평소와 비슷한 시간대";
                case TIME_21_24 -> "늦은 시간에도 주문 흐름이 평소와 비슷한 편";
            };
            case LOW -> switch (timeBand) {
                case TIME_00_06 -> "심야 주문 흐름이 뜸한 시간대";
                case TIME_06_11 -> "오전 주문 흐름이 한산한 시간대";
                case TIME_11_14 -> "점심 주문 흐름이 한산한 시간대";
                case TIME_14_17 -> "오후 주문 흐름이 한산한 시간대";
                case TIME_17_21 -> "저녁 주문 흐름이 한산한 시간대";
                case TIME_21_24 -> "늦은 시간, 주문 흐름이 잦아드는 구간";
            };
            case CLOSED -> switch (timeBand) {
                case TIME_00_06 -> "주문 흐름이 가장 잦아드는 심야 시간대";
                case TIME_06_11 -> "오전 주문 흐름이 매우 한산한 시간대";
                case TIME_11_14 -> "점심 주문 흐름이 매우 한산한 시간대";
                case TIME_14_17 -> "오후 주문 흐름이 매우 한산한 시간대";
                case TIME_17_21 -> "저녁 주문 흐름이 매우 한산한 시간대";
                case TIME_21_24 -> "늦은 시간, 주문 흐름이 크게 잦아드는 구간";
            };
        };
    }

    public String createLocalPatternDescription(DayDemandLevel dayDemandLevel,
                                                int appliedDayScore,
                                                String businessTypeName,
                                                DayOfWeek currentDayOfWeek) {
        String day = dayDemandLevel == DayDemandLevel.HOLIDAY
                ? "공휴일"
                : koreanDayOfWeek(currentDayOfWeek);

        String subject = businessTypeName == null || businessTypeName.isBlank()
                ? day + " 주문 흐름은"
                : "이 지역의 " + businessTypeName + " 업종은 " + day + " 흐름이";

        if (appliedDayScore > 0) {
            return subject + " 비교적 활발한 편";
        }

        if (appliedDayScore < 0) {
            return subject + " 비교적 한산한 편";
        }

        return subject + " 평소와 비슷한 편";
    }

    // 구간 경계는 ScoreStatusLevel이 단일 기준이다. 여기서는 문구만 정한다.
    public String calculateStatus(int score) {
        return switch (ScoreStatusLevel.from(score)) {
            case VERY_HIGH -> "매우 높음 · 수요 급등 구간";
            case HIGH -> "높음 · 높은 수요 구간";
            case MEDIUM -> "보통 · 평균 수요 구간";
            case LOW -> "낮음 · 수요 둔화 구간";
            case CLOSED -> "매우 낮음 · 한산한 수요 구간";
        };
    }

    // calculateStatus와 같은 구간을 써야 상태 라벨과 안내 문구가 어긋나지 않는다.
    // 경계를 여기 따로 두면 한쪽만 바뀌었을 때 "높음"인데 보수적으로 준비하라는 문구가 나온다.
    public String createMessage(int score) {
        return switch (ScoreStatusLevel.from(score)) {
            case VERY_HIGH -> "현재 배달 수요가 매우 높은 편입니다.";
            case HIGH -> "현재 배달 수요가 높은 편입니다.";
            case MEDIUM -> "현재 배달 수요가 평소 수준입니다.";
            case LOW -> "현재 배달 수요가 낮은 편입니다.";
            case CLOSED -> "현재 배달 수요가 매우 낮은 편입니다.";
        };
    }

    /** 현재 상태 설명 뒤에 가까운 1~3시간 전망을 붙인다. */
    public String createMessage(int currentScore, List<Integer> futureScores) {
        String currentMessage = createMessage(currentScore);
        String forecastMessage = createForecastMessage(currentScore, futureScores);

        if (forecastMessage.isEmpty()) {
            return currentMessage;
        }

        return currentMessage + " " + forecastMessage;
    }

    public String createForecastMessage(int currentScore, List<Integer> futureScores) {
        if (futureScores == null || futureScores.isEmpty()) {
            return "";
        }

        List<Integer> scores = futureScores.stream()
                .filter(Objects::nonNull)
                .limit(3)
                .toList();

        if (scores.isEmpty()) {
            return "";
        }

        int rise = scores.stream().mapToInt(Integer::intValue).max().orElse(currentScore)
                - currentScore;
        int fall = scores.stream().mapToInt(Integer::intValue).min().orElse(currentScore)
                - currentScore;

        boolean hasRise = rise >= 8;
        boolean hasFall = fall <= -8;

        if (hasRise && (!hasFall || rise >= Math.abs(fall))) {
            return createRisingForecastMessage(rise);
        }

        if (hasFall) {
            return createFallingForecastMessage(fall);
        }

        return "앞으로 1~3시간은 지금과 비슷한 흐름이 이어질 전망입니다.";
    }

    private String createRisingForecastMessage(int delta) {
        if (delta >= 25) {
            return "앞으로 1~3시간 안에 배달온도가 크게 오를 전망입니다. 피크에 대비해 미리 준비해 두세요.";
        }

        if (delta >= 15) {
            return "앞으로 1~3시간 안에 배달온도가 눈에 띄게 오를 전망입니다. 주문 증가에 대비해 준비해 두세요.";
        }

        return "앞으로 1~3시간 동안 배달온도가 다소 오를 전망입니다.";
    }

    private String createFallingForecastMessage(int delta) {
        if (delta <= -24) {
            return "앞으로 1~3시간 안에 배달온도가 크게 낮아질 전망입니다. 이후 추가 준비는 신중하게 가져가세요.";
        }

        if (delta <= -15) {
            return "앞으로 1~3시간 안에 배달온도가 눈에 띄게 낮아질 전망입니다. 추가 준비는 신중하게 가져가세요.";
        }

        return "앞으로 1~3시간 동안 배달온도가 다소 낮아질 전망입니다.";
    }

    public String createAirQualityDetail(CurrentAirQualityObservation airQuality) {
        if (airQuality == null) {
            return "대기질 정보 없음";
        }

        return "미세먼지 " + formatNullableValue(airQuality.getPm10Value())
                + ", 초미세먼지 " + formatNullableValue(airQuality.getPm25Value());
    }

    private String koreanDayOfWeek(DayOfWeek dayOfWeek) {
        if (dayOfWeek == null) {
            return "오늘";
        }

        return switch (dayOfWeek) {
            case MONDAY -> "월요일";
            case TUESDAY -> "화요일";
            case WEDNESDAY -> "수요일";
            case THURSDAY -> "목요일";
            case FRIDAY -> "금요일";
            case SATURDAY -> "토요일";
            case SUNDAY -> "일요일";
        };
    }

    private String formatNullableValue(Object value) {
        if (value == null) {
            return "정보 없음";
        }

        return value.toString();
    }
}
