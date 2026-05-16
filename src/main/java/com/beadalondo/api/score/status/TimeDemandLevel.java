package com.beadalondo.api.score.status;

public enum TimeDemandLevel {
    VERY_HIGH(30, "피크 수요 시간대", "↑↑"),
    HIGH(25, "높은 수요 시간대", "↑"),
    MEDIUM(15, "보통 수요 시간대", "•"),
    LOW(10, "낮은 수요 시간대", "↓"),
    CLOSED(0, "배달앱 비활성 시간대", "↓↓");

    private final int weight;
    private final String timeDescription;
    private String timeFactor;

    TimeDemandLevel(int weight, String timeDescription, String timeFactor) {
        this.weight = weight;
        this.timeDescription = timeDescription;
        this.timeFactor = timeFactor;
    }

    public int getWeight() {
        return weight;
    }

    public String getTimeDescription() {
        return timeDescription;
    }

    public String getTimeFactor() {
        return timeFactor;
    }

}