package com.beadalondo.api.dashboard.dto;

import com.beadalondo.api.score.ScoreResult;
import com.beadalondo.api.store.domain.Store;

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

    public DashboardView(Store store,
                         int score,
                         String status,
                         String message,
                         String timeFactor,
                         String timeDescription,
                         String dayFactor,
                         String dayDescription,
                         String currentWeatherFactor,
                         String currentWeatherDescription) {
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
    }

    public static DashboardView from(Store store, ScoreResult scoreResult) {
        return new DashboardView(store,
                scoreResult.getScore(),
                scoreResult.getStatus(),
                scoreResult.getMessage(),
                scoreResult.getTimeFactor(),
                scoreResult.getTimeDescription(),
                scoreResult.getDayFactor(),
                scoreResult.getDayDescription(),
                scoreResult.getCurrentWeatherFactor(),
                scoreResult.getCurrentWeatherDescription());
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
}
