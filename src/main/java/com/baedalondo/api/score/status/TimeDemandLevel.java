package com.baedalondo.api.score.status;

public enum TimeDemandLevel {
    VERY_HIGH(30, "주문 흐름이 특히 강한 시간대", "↑↑"),
    HIGH(25, "주문 흐름이 강한 시간대", "↑"),
    MEDIUM(15, "평소와 비슷한 주문 시간대", "•"),
    LOW(10, "주문 흐름이 뜸한 시간대", "↓"),
    CLOSED(0, "주문 흐름이 가장 잦아드는 시간대", "↓↓");

    private final int weight;
    private final String timeDescription;
    private final String timeFactor;

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
