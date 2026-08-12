package com.baedalondo.api.score;

import com.baedalondo.api.score.status.ScoreStatusLevel;


public class ScoreResult {

    private final int score;
    private final String status;
    private final String message;
    private final String timeFactor;
    private final String dayFactor;
    private final String timeDescription;
    private final String dayDescription;
    private final String currentWeatherFactor;
    private final String currentWeatherDescription;
    private final String airQualityFactor;
    private final String airQualityDescription;
    private final String airQualityDetail;

    public ScoreResult(int score,
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
                       String airQualityDetail) {
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
    }

    public int getScore() {
        return score;
    }

    public String getStatus() {
        return status;
    }

    /**
     * 표시 문구와 무관한 점수 구간 코드.
     * 화면 스타일은 status 문자열이 아니라 이 값을 기준으로 결정한다.
     */
    public ScoreStatusLevel getStatusLevel() {
        return ScoreStatusLevel.from(score);
    }

    public String getMessage() {
        return message;
    }

    public String getTimeFactor() {
        return timeFactor;
    }

    public String getTimeDescription() {
        return timeDescription;
    }

    public String getDayFactor() {
        return dayFactor;
    }

    public String getDayDescription() {
        return dayDescription;
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
