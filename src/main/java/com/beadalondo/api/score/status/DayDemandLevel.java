package com.beadalondo.api.score.status;

public enum DayDemandLevel {
    WEEKDAY(0, "평일", "•"),
    FRIDAY(5, "금요일", "↑"),
    WEEKEND(10, "주말", "↑"),
    HOLIDAY(10, "공휴일", "↑");

    private final int weight;
    private final String dayDescription;
    private final String dayFactor;

    DayDemandLevel(int weight, String dayDescription, String dayFactor) {
        this.weight = weight;
        this.dayDescription = dayDescription;
        this.dayFactor = dayFactor;
    }

    public int getWeight() {
        return weight;
    }

    public String getDayDescription() {
        return dayDescription;
    }

    public String getDayFactor() {
        return dayFactor;
    }
}
