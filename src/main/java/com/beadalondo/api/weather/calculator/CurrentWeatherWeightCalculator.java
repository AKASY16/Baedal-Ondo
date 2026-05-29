package com.beadalondo.api.weather.calculator;

import com.beadalondo.api.weather.domain.CurrentWeatherObservation;
import com.beadalondo.api.weather.domain.WeatherScoreResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CurrentWeatherWeightCalculator {
    public WeatherScoreResult calculate(CurrentWeatherObservation weather) {
        int rainfallScore = calculateRainfallScore(weather.getRainfall());
        int precipitationTypeScore = calculatePrecipitationTypeScore(weather.getPrecipitationType());
        int windSpeedScore = calculateWindSpeedScore(weather.getWindSpeed());
        int temperatureScore = calculateTemperatureScore(weather.getTemperature());

        int weatherScore = rainfallScore + precipitationTypeScore + windSpeedScore + temperatureScore;
        List<String> factors = new ArrayList<>();

        addFactor(factors, rainfallScore, "강수량");
        addFactor(factors, precipitationTypeScore, "강수형태");
        addFactor(factors, windSpeedScore, "풍속");
        addFactor(factors, temperatureScore, "기온");

        if (factors.isEmpty()) {
            factors.add("날씨 영향 없음");
        }

        return new WeatherScoreResult(
                weatherScore,
                factors,
                String.join(", ", factors)
        );
    }

    private int calculateRainfallScore(double rainfall) {
        if (rainfall <= 0) {
            return 0;
        }
        if (rainfall < 1) {
            return 1;
        }
        if (rainfall < 3) {
            return 2;
        }
        if (rainfall < 15) {
            return 3;
        }
        if (rainfall < 30) {
            return 4;
        }
        return 5;
    }

    private int calculatePrecipitationTypeScore(int precipitationType) {
        return switch (precipitationType) {
            case 5 -> 1;
            case 2, 6 -> 2;
            case 7 -> 3;
            case 3 -> 4;
            default -> 0;
        };
    }

    private int calculateWindSpeedScore(double windSpeed) {
        if (windSpeed < 4) {
            return 0;
        }
        if (windSpeed < 9) {
            return 1;
        }
        if (windSpeed < 14) {
            return 2;
        }
        if (windSpeed < 21) {
            return 4;
        }
        return 5;
    }

    private int calculateTemperatureScore(double temperature) {
        if (temperature >= 10 && temperature <= 25) {
            return 0;
        }
        if ((temperature >= 5 && temperature < 10) || (temperature > 25 && temperature < 28)) {
            return 1;
        }
        if ((temperature >= 0 && temperature < 5) || (temperature >= 28 && temperature < 31)) {
            return 2;
        }
        return 3;
    }

    private void addFactor(List<String> factors, int score, String factor) {
        if (score > 0) {
            factors.add(factor);
        }
    }
}
