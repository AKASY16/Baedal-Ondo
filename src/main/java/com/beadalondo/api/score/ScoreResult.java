package com.beadalondo.api.score;


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

    public ScoreResult(int score,
                       String status,
                       String message,
                       String timeFactor,
                       String timeDescription,
                       String dayFactor,
                       String dayDescription,
                       String currentWeatherFactor,
                       String currentWeatherDescription) {
        this.score = score;
        this.status = status;
        this.message = message;
        this.timeFactor = timeFactor;
        this.timeDescription = timeDescription;
        this.dayFactor = dayFactor;
        this.dayDescription = dayDescription;
        this.currentWeatherFactor = currentWeatherFactor;
        this.currentWeatherDescription = currentWeatherDescription;
    }

    public int getScore() {
        return score;
    }

    public String getStatus() {
        return status;
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
}