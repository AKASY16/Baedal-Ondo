package com.beadalondo.api.score.status;

public enum DayDemandLevel {
    WEEKDAY(0, "평일", "•"),
    FRIDAY(5, "금요일", "↑"),
    WEEKEND(10, "주말", "↑");

    private int weight;
    private String dayDescription;
    private String dayFactor;


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
