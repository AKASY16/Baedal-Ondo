package com.baedalondo.api.score.factory;
import com.baedalondo.api.airquality.domain.CurrentAirQualityObservation;
import com.baedalondo.api.weather.domain.WeatherScoreResult;
import org.springframework.stereotype.Component;


@Component
public class ScoreMessageFactory {


    public ScoreMessageFactory(){

    }

    public String createWeatherFactor(WeatherScoreResult weatherScoreResult) {
        if (weatherScoreResult.getWeatherScore() <= 0) {
            return "•";
        }

        return "↑";
    }

    /**
     요일 화살표는 DayDemandLevel이 아니라 실제 적용된 요일 점수에서 만든다.
     상권에 따라 주말이나 금요일도 음수가 될 수 있어서,
     enum에 고정된 화살표를 쓰면 표시와 점수 방향이 어긋난다.
     */
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

    public String calculateStatus(int score) {
        if (score >= 80) {
            return "상 · 수요 급등 구간";
        }

        if (score >= 60) {
            return "상 · 높은 수요 구간";
        }

        if (score >= 40) {
            return "중 · 평균 수요 구간";
        }

        if (score >= 20) {
            return "하 · 수요 둔화 구간";
        }

        return "마감 · 매우 낮은 수요 구간";
    }

    public String createMessage(int score) {
        if (score >= 80) {
            return "오늘은 배달 수요가 높을 가능성이 큽니다. 연장 영업과 재료 추가 준비를 고려하세요.";
        }

        if (score >= 60) {
            return "평소보다 주문이 늘 수 있습니다. 피크 시간대 준비를 조금 더 여유 있게 가져가세요.";
        }

        if (score >= 40) {
            return "평균적인 수요가 예상됩니다. 평상시 영업 흐름을 유지하세요.";
        }

        if (score >= 20) {
            return "현재 수요가 낮은 편입니다. 식자재 선조리와 인력 운영을 보수적으로 가져가세요.";
        }

        return "기대 수요가 매우 낮습니다. 운영 비용을 고려해 보수적으로 준비하세요.";
    }

    public String createAirQualityDetail(CurrentAirQualityObservation airQuality) {
        if (airQuality == null) {
            return "대기질 정보 없음";
        }

        return "미세먼지 " + formatNullableValue(airQuality.getPm10Value())
                + ", 초미세먼지 " + formatNullableValue(airQuality.getPm25Value())
                + ", 오존 " + formatNullableValue(airQuality.getO3Value());
    }

    private String formatNullableValue(Object value) {
        if (value == null) {
            return "정보 없음";
        }

        return value.toString();
    }









}
