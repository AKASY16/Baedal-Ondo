package com.beadalondo.api.score.calculator;

import com.beadalondo.api.score.status.CurrentWeatherDemandLevel;
import com.beadalondo.api.weather.CurrentWeatherObservation;
import org.springframework.stereotype.Component;

@Component
public class CurrentWeatherWeightCalculator {

    public CurrentWeatherDemandLevel calculate(CurrentWeatherObservation weather) {

        if (isSnow(weather)) {
            return CurrentWeatherDemandLevel.SNOW;
        }

        if (isRain(weather)) {
            return CurrentWeatherDemandLevel.RAIN;
        }

        if (isExtremeTemperature(weather)) {
            return CurrentWeatherDemandLevel.EXTREME_TEMP;
        }

        if (isHumid(weather)) {
            return CurrentWeatherDemandLevel.HUMID;
        }

        if (isStrongWind(weather)) {
            return CurrentWeatherDemandLevel.STRONG_WIND;
        }

        return CurrentWeatherDemandLevel.NORMAL;
    }

    private boolean isRain(CurrentWeatherObservation weather) {
        return weather.getPrecipitationType() == 1
                || weather.getPrecipitationType() == 2
                || weather.getPrecipitationType() == 5
                || weather.getPrecipitationType() == 6
                || weather.getRainfall() > 0;
    }

    private boolean isSnow(CurrentWeatherObservation weather) {
        return weather.getPrecipitationType() == 3
                || weather.getPrecipitationType() == 7;
    }

    private boolean isStrongWind(CurrentWeatherObservation weather) {
        return weather.getWindSpeed() >= 8.0;
    }

    private boolean isExtremeTemperature(CurrentWeatherObservation weather) {
        return weather.getTemperature() >= 30.0
                || weather.getTemperature() <= 0.0;
    }

    private boolean isHumid(CurrentWeatherObservation weather) {
        return weather.getHumidity() >= 90;
    }
}