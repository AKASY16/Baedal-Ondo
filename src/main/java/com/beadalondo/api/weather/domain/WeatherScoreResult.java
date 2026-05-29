package com.beadalondo.api.weather.domain;

import java.util.List;

public class WeatherScoreResult {
    private final int weatherScore;
    private final List<String> factors;
    private final String description;

    public WeatherScoreResult(int weatherScore, List<String> factors, String description) {
        this.weatherScore = weatherScore;
        this.factors = factors;
        this.description = description;
    }

    public int getWeatherScore() {
        return weatherScore;
    }

    public List<String> getFactors() {
        return factors;
    }

    public String getDescription() {
        return description;
    }

}
