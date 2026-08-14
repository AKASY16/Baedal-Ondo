package com.baedalondo.api.score.status;

public enum DayDemandLevel {
    WEEKDAY("평일", "•"),
    FRIDAY("금요일", "↑"),
    WEEKEND("주말", "↑"),
    HOLIDAY("공휴일", "↑");

    private final String dayDescription;
    private final String dayFactor;

    DayDemandLevel(String dayDescription, String dayFactor) {
        this.dayDescription = dayDescription;
        this.dayFactor = dayFactor;
    }

    public String getDayDescription() {
        return dayDescription;
    }

    public String getDayFactor() {
        return dayFactor;
    }
}
