package com.beadalondo.api.weather.client;

import com.beadalondo.api.weather.CurrentWeatherObservation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Component
public class KmaCurrentWeatherClient {

    private final RestClient restClient;
    private final String authKey;

    public KmaCurrentWeatherClient(RestClient.Builder restClientBuilder,
                                   @Value("${kma.api.base-url}") String baseUrl,
                                   @Value("${kma.api.auth-key}") String authKey) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
        this.authKey = authKey;
    }

    public CurrentWeatherObservation getCurrentWeather(int nx, int ny) {
        LocalDateTime baseDateTime = getSafeBaseDateTime();

        String baseDate = baseDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseTime = baseDateTime.format(DateTimeFormatter.ofPattern("HH00"));

        JsonNode root = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/getUltraSrtNcst")
                        .queryParam("pageNo", 1)
                        .queryParam("numOfRows", 1000)
                        .queryParam("dataType", "JSON")
                        .queryParam("base_date", baseDate)
                        .queryParam("base_time", baseTime)
                        .queryParam("nx", nx)
                        .queryParam("ny", ny)
                        .queryParam("authKey", authKey)
                        .build())
                .retrieve()
                .body(JsonNode.class);

        return parseCurrentWeather(root);
    }

    private LocalDateTime getSafeBaseDateTime() {
        return LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                .minusHours(1)
                .truncatedTo(ChronoUnit.HOURS);
    }

    private CurrentWeatherObservation parseCurrentWeather(JsonNode root) {
        if (root == null) {
            throw new IllegalStateException("기상청 API가 비어있습니다.");
        }

        String resultCode = root.path("response")
                .path("header")
                .path("resultCode")
                .asString();

        if (!"00".equals(resultCode)) {
            String resultMsg = root.path("response")
                    .path("header")
                    .path("resultMsg")
                    .asString("");

            throw new IllegalStateException("기상청 API 에러, 코드: " + resultMsg);
        }

        JsonNode items = root.path("response")
                .path("body")
                .path("items")
                .path("item");

        int precipitationType = 0;
        double rainfall = 0.0;
        double temperature = 0.0;
        int humidity = 0;
        double windSpeed = 0.0;

        for (JsonNode item : items.values()) {
            String category = item.path("category").asString();
            String value = item.path("obsrValue").asString();

            switch (category) {
                case "PTY" -> precipitationType = Integer.parseInt(value);
                case "RN1" -> rainfall = Double.parseDouble(value);
                case "T1H" -> temperature = Double.parseDouble(value);
                case "REH" -> humidity = Integer.parseInt(value);
                case "WSD" -> windSpeed = Double.parseDouble(value);
            }
        }

        return new CurrentWeatherObservation(
                precipitationType,
                rainfall,
                temperature,
                humidity,
                windSpeed
        );
    }
}
