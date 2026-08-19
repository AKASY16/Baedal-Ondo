package com.baedalondo.api.dashboard.dto;

import com.baedalondo.api.score.ScoreResult;
import com.baedalondo.api.score.status.ScoreStatusLevel;
import com.baedalondo.api.store.domain.Store;

import java.time.LocalDateTime;
import java.util.Map;

public class DashboardView {
    private final Store store;
    private final int score;
    private final String status;
    private final String message;
    private final String timeFactor;
    private final String timeDescription;
    private final String dayFactor;
    private final String dayDescription;
    private final String currentWeatherFactor;
    private final String currentWeatherDescription;
    private final String airQualityFactor;
    private final String airQualityDescription;
    private final String airQualityDetail;
    private final Map<LocalDateTime, ScoreResult> forecastScores;

    public DashboardView(Store store,
                         int score,
                         String status,
                         String message,
                         String timeFactor,
                         String timeDescription,
                         String dayFactor,
                         String dayDescription,
                         String currentWeatherFactor,
                         String currentWeatherDescription,
                         String airQualityFactor,
                         String airQualityDescription,
                         String airQualityDetail,
                         Map<LocalDateTime, ScoreResult> forecastScores) {
        this.store = store;
        this.score = score;
        this.status = status;
        this.message = message;
        this.timeFactor = timeFactor;
        this.timeDescription = timeDescription;
        this.dayFactor = dayFactor;
        this.dayDescription = dayDescription;
        this.currentWeatherFactor = currentWeatherFactor;
        this.currentWeatherDescription = currentWeatherDescription;
        this.airQualityFactor = airQualityFactor;
        this.airQualityDescription = airQualityDescription;
        this.airQualityDetail = airQualityDetail;
        this.forecastScores = forecastScores;
    }

    public static DashboardView from(
            Store store,
            ScoreResult scoreResult,
            Map<LocalDateTime, ScoreResult> forecastScores
    ) {
        return new DashboardView(
                store,
                scoreResult.getScore(),
                scoreResult.getStatus(),
                scoreResult.getMessage(),
                scoreResult.getTimeFactor(),
                scoreResult.getTimeDescription(),
                scoreResult.getDayFactor(),
                scoreResult.getDayDescription(),
                scoreResult.getCurrentWeatherFactor(),
                scoreResult.getCurrentWeatherDescription(),
                scoreResult.getAirQualityFactor(),
                scoreResult.getAirQualityDescription(),
                scoreResult.getAirQualityDetail(),
                forecastScores
        );
    }


    public Map<LocalDateTime, ScoreResult> getForecastScores() {
        return forecastScores;
    }

    public Store getStore() {
        return store;
    }

    public int getScore() {
        return score;
    }

    public String getStatus() {
        return status;
    }

    /**
     * 화면 스타일 결정용 점수 구간 코드.
     * 템플릿이 status 문구를 파싱하지 않도록 별도로 노출한다.
     */
    public ScoreStatusLevel getStatusLevel() {
        return ScoreStatusLevel.from(score);
    }

    public String getMessage() {
        return message;
    }

    public String getDayFactor() {
        return dayFactor;
    }

    public String getTimeFactor() {
        return timeFactor;
    }

    public String getDayDescription() {
        return dayDescription;
    }

    public String getTimeDescription() {
        return timeDescription;
    }

    public String getCurrentWeatherFactor() {
        return currentWeatherFactor;
    }

    public String getCurrentWeatherDescription() {
        return currentWeatherDescription;
    }

    public String getAirQualityFactor() {
        return airQualityFactor;
    }

    public String getAirQualityDescription() {
        return airQualityDescription;
    }

    public String getAirQualityDetail() {
        return airQualityDetail;
    }
}
