package com.baedalondo.api.weather.calculator;

import com.baedalondo.api.weather.domain.WeatherMeasurement;
import com.baedalondo.api.weather.domain.WeatherScoreResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 날씨 점수와 설명 문구를 만든다.
 *
 * 실황과 예보가 같은 기상청 카테고리를 쓰므로 계산 규칙도 같다.
 * 임계값과 문구가 한 곳에만 있도록 이 클래스가 실제 계산을 전담하고,
 * CurrentWeatherWeightCalculator와 ForecastWeatherWeightCalculator는
 * 각자의 관측 타입을 받아 여기로 넘긴다.
 */
@Component
public class WeatherWeightCalculator {
    public WeatherScoreResult calculate(WeatherMeasurement weather) {
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
                createDescription(weather, rainfallScore, precipitationTypeScore, windSpeedScore, temperatureScore)
        );
    }

    private String createDescription(WeatherMeasurement weather,
                                     int rainfallScore,
                                     int precipitationTypeScore,
                                     int windSpeedScore,
                                     int temperatureScore) {
        boolean precipitationImpact = rainfallScore > 0 || precipitationTypeScore > 0;
        boolean windImpact = windSpeedScore > 0;
        boolean temperatureImpact = temperatureScore > 0;

        if (!precipitationImpact && !windImpact && !temperatureImpact) {
            return "외출에 큰 불편이 없는 날씨";
        }

        String precipitation = precipitationDescription(weather.getPrecipitationType());
        String precipitationWithAnd = precipitationWithAnd(precipitation);
        String wind = windDescription(windSpeedScore);
        String temperature = temperatureDescription(weather.getTemperature(), temperatureScore);

        if (precipitationImpact && windImpact && temperatureImpact) {
            return precipitationWithAnd + wind + ", " + temperature + "이 겹친 날씨";
        }

        if (precipitationImpact && windImpact) {
            return precipitationWithAnd + wind + "이 겹친 날씨";
        }

        if (precipitationImpact && temperatureImpact) {
            return precipitationWithAnd + temperature + "이 겹친 날씨";
        }

        if (windImpact && temperatureImpact) {
            return wind + "과 " + temperature + "이 겹친 날씨";
        }

        if (precipitationImpact) {
            return precipitationOnlyDescription(
                    precipitation,
                    Math.max(rainfallScore, precipitationTypeScore)
            );
        }

        if (windImpact) {
            if (windSpeedScore == 1) {
                return "바람이 다소 강한 날씨";
            }
            if (windSpeedScore == 2) {
                return "강한 바람으로 외출이 다소 불편한 날씨";
            }
            return "매우 강한 바람으로 외출이 불편한 날씨";
        }

        if (temperatureScore == 1) {
            return weather.getTemperature() < 10
                    ? "기온이 조금 낮은 편"
                    : "기온이 조금 높은 편";
        }

        if (temperatureScore == 2) {
            return temperature + "으로 외출이 다소 불편한 날씨";
        }

        return temperature + "으로 외출이 불편한 날씨";
    }

    private String precipitationOnlyDescription(String precipitation, int severity) {
        if (severity == 1) {
            return switch (precipitation) {
                case "눈" -> "약한 눈이 내리는 날씨";
                case "비·눈" -> "약한 비와 눈이 내리는 날씨";
                default -> "약한 비가 내리는 날씨";
            };
        }

        String cause = switch (precipitation) {
            case "눈" -> "눈으로 ";
            case "비·눈" -> "비와 눈으로 ";
            default -> "비로 ";
        };

        if (severity == 2) {
            return cause + "외출이 다소 불편한 날씨";
        }

        return cause + "외출이 불편한 날씨";
    }

    private String precipitationWithAnd(String precipitation) {
        return switch (precipitation) {
            case "눈" -> "눈과 ";
            case "비·눈" -> "비와 눈, ";
            default -> "비와 ";
        };
    }

    private String windDescription(int windSpeedScore) {
        if (windSpeedScore == 1) {
            return "다소 강한 바람";
        }
        if (windSpeedScore == 2) {
            return "강한 바람";
        }
        return "매우 강한 바람";
    }

    private String temperatureDescription(double temperature, int temperatureScore) {
        boolean isCold = temperature < 10;

        if (temperatureScore == 1) {
            return isCold ? "조금 낮은 기온" : "조금 높은 기온";
        }
        if (temperatureScore == 2) {
            return isCold ? "낮은 기온" : "높은 기온";
        }
        return isCold ? "매우 낮은 기온" : "매우 높은 기온";
    }

    private String precipitationDescription(int precipitationType) {
        return switch (precipitationType) {
            case 2, 6 -> "비·눈";
            case 3, 7 -> "눈";
            default -> "비";
        };
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
