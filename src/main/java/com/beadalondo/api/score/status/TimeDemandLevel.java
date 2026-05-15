package com.beadalondo.api.score.status;

public enum TimeDemandLevel {
    CLOSED(0, "영업 비권장 시간"),
    LOW(10, "낮은 수요 시간대"),
    MEDIUM(15, "보통 수요 시간대"),
    HIGH(25, "높은 수요 시간대"),
    VERY_HIGH(30, "피크 수요 시간대");

    private final int weight;
    private final String description;

    TimeDemandLevel(int weight, String description) {
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