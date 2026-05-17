package com.beadalondo.api.weather.domain;

public class CurrentWeatherObservation {

    private final int precipitationType; // PTY
    private final double rainfall;       // RN1
    private final double temperature;    // T1H
    private final int humidity;          // REH
    private final double windSpeed;      // WSD

    public CurrentWeatherObservation(int precipitationType,
                                     double rainfall,
                                     double temperature,
                                     int humidity,
                                     double windSpeed) {
        this.precipitationType = precipitationType;
        this.rainfall = rainfall;
        this.temperature = temperature;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
    }

    public int getPrecipitationType() {
        return precipitationType;
    }

    public double getRainfall() {
        return rainfall;
    }

    public double getTemperature() {
        return temperature;
    }

    public int getHumidity() {
        return humidity;
    }

    public double getWindSpeed() {
        return windSpeed;
    }
}