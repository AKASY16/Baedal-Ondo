package com.beadalondo.api.score.factory;
import com.beadalondo.api.airquality.domain.CurrentAirQualityObservation;
import com.beadalondo.api.weather.domain.WeatherScoreResult;
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

        if (score >= 40) {
            return "중 · 평시 운영 구간";
        }

        if (score >= 20) {
            return "하 · 수요 둔화 구간";
        }

        return "마감 · 조기 마감 검토";
    }

    public String createMessage(int score) {
        if (score >= 80) {
            return "오늘은 배달 수요가 높을 가능성이 큽니다. 연장 영업과 재료 추가 준비를 고려하세요.";
        }

        if (score >= 40) {
            return "평상시 영업을 유지하세요. 피크 시간대 주문 흐름을 지켜보세요.";
        }

        if (score >= 20) {
            return "현재 수요가 낮은 편입니다. 식자재 선조리와 인력 운영을 보수적으로 가져가세요.";
        }

        return "기대 수요가 매우 낮습니다. 유지 비용을 고려해 조기 마감을 검토하세요.";
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
