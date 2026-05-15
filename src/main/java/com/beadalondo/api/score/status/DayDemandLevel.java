package com.beadalondo.api.score.status;

public enum DayDemandLevel {
    WEEKDAY(0, "평일"),
    WEEKEND(10, "주말"),
    FRIDAY(15, "금요일");

    private int weight;
    private String description;

    DayDemandLevel(int weight, String description) {
        this.weight = weight;
        this.description = description;
    }

    public int getWeight() {
        return weight;
    }

    public String getDescription() {
        return description;
    }

}
