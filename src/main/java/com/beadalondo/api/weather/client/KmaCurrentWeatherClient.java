package com.beadalondo.api.weather.client;
import com.beadalondo.api.weather.domain.CurrentWeatherObservation;
import com.beadalondo.api.weather.exception.KmaWeatherApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

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

    public CurrentWeatherObservation getCurrentWeather(int nx, int ny, String baseDate, String baseTime) {
        try {
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
        } catch (RestClientException | NumberFormatException e) {
            throw new KmaWeatherApiException("기상청 API 호출 또는 응답 처리 중 오류가 발생했습니다.", e);
        }
    }



    private CurrentWeatherObservation parseCurrentWeather(JsonNode root) {
        if (root == null) {
            throw new KmaWeatherApiException("기상청 API가 비어있습니다.");
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

            throw new KmaWeatherApiException("기상청 API 에러, 코드: " + resultMsg);
        }

        JsonNode items = root.path("response")
                .path("body")
                .path("items")
                .path("item");

        if (!items.isArray() || items.isEmpty()) {
            throw new KmaWeatherApiException("기상청 API 응답에 날씨 관측 항목이 없습니다.");
        }

        int precipitationType = 0;
        double rainfall = 0.0;
        double temperature = 0.0;
        int humidity = 0;
        double windSpeed = 0.0;

        boolean hasPty = false;
        boolean hasRn1 = false;
        boolean hasT1h = false;
        boolean hasReh = false;
        boolean hasWsd = false;

        for (JsonNode item : items.values()) {
            String category = item.path("category").asString();
            String value = item.path("obsrValue").asString();

            switch (category) {
                case "PTY" -> {
                    precipitationType = Integer.parseInt(value);
                    hasPty = true;
                }
                case "RN1" -> {
                    rainfall = Double.parseDouble(value);
                    hasRn1 = true;
                }
                case "T1H" -> {
                    temperature = Double.parseDouble(value);
                    hasT1h = true;
                }
                case "REH" -> {
                    humidity = Integer.parseInt(value);
                    hasReh = true;
                }
                case "WSD" -> {
                    windSpeed = Double.parseDouble(value);
                    hasWsd = true;
                }
            }


        }

        if (!(hasPty && hasRn1 && hasT1h && hasReh && hasWsd)) {
            throw new KmaWeatherApiException("기상청 API 응답에 필수 날씨 항목이 누락되었습니다.");
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
