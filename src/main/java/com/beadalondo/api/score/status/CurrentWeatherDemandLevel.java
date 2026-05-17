package com.beadalondo.api.score.status;

public enum CurrentWeatherDemandLevel {
    NORMAL(0, "날씨 영향 낮음", "•"),
    HUMID(5, "습도 높음", "↑"),
    EXTREME_TEMP(10, "폭염/한파", "↑"),
    STRONG_WIND(10, "강풍", "↑"),
    RAIN(20, "비", "↑↑"),
    SNOW(25, "눈", "↑↑"),
    UNAVAILABLE(0,"날씨 정보 없음", "날씨 정보 없음");

    private final int weight;
    private final String description;
    private final String factor;

    CurrentWeatherDemandLevel(int weight, String description, String factor) {
        this.weight = weight;
        this.description = description;
        this.factor = factor;
    }

    public int getWeight() {
        return weight;
    }

    public String getDescription() {
        return description;
    }

    public String getFactor() {
        return factor;
    }
}
