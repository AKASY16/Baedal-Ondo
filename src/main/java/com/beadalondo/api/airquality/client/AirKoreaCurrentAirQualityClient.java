package com.beadalondo.api.airquality.client;

import com.beadalondo.api.airquality.domain.CurrentAirQualityObservation;
import com.beadalondo.api.airquality.exception.AirKoreaApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;

@Component
public class AirKoreaCurrentAirQualityClient {
    private final RestClient restClient;
    private final String authKey;

    public AirKoreaCurrentAirQualityClient(RestClient.Builder restClientBuilder,
                                           @Value("http://apis.data.go.kr/B552584/ArpltnInforInqireSvc") String baseUrl,
                                           @Value("${dataportal.api.auth-key}") String authKey) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
        this.authKey = authKey;
    }

    public List<CurrentAirQualityObservation> getCurrentAirQualities(String sidoName) {
        try {
            JsonNode root = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/getCtprvnRltmMesureDnsty")
                            .queryParam("sidoName", sidoName)
                            .queryParam("pageNo", 1)
                            .queryParam("numOfRows", 100)
                            .queryParam("returnType", "json")
                            .queryParam("serviceKey", authKey)
                            .queryParam("ver", "1.5")
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            return parseCurrentAirQualities(root);

        } catch (RestClientException | NumberFormatException | DateTimeException e) {
            throw new AirKoreaApiException("에어코리아 API 호출 또는 응답 처리 중 오류가 발생했습니다.", e);
        }
    }


    public List<CurrentAirQualityObservation> parseCurrentAirQualities(JsonNode root) {

        if (root == null) {
            throw new AirKoreaApiException("에어코리아 API 응답이 비어있습니다.");
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

            throw new AirKoreaApiException(
                    "에어코리아 API 에러, resultCode=" + resultCode + ", resultMsg=" + resultMsg
            );
        }

        JsonNode items = root
                .path("response")
                .path("body")
                .path("items");

        if (!items.isArray() || items.isEmpty()) {
            throw new AirKoreaApiException("에어코리아 API 응답에 내용이 없습니다.");
        }

        List<CurrentAirQualityObservation> observations = new ArrayList<>();

        for (JsonNode item : items) {
            String mangName = item.path("mangName").asString();

            if ("도시대기".equals(mangName) && hasValidMainValues(item)) {
                observations.add(parseItemToObservation(item));
            }
        }

        if (observations.isEmpty()) {
            throw new AirKoreaApiException("사용 가능한 도시대기 측정소 데이터가 없습니다.");
        }

        return observations;
    }


    private Integer parseNullableInt(JsonNode item, String fieldName) {
        String value = item.path(fieldName).asString();

        if (value == null || value.isBlank() || "-".equals(value)) {
            return null;
        }

        return parseInt(value);
    }

    private Double parseNullableDouble(JsonNode item, String fieldName) {
        String value = item.path(fieldName).asString();

        if (value == null || value.isBlank() || "-".equals(value)) {
            return null;
        }

        return parseDouble(value);
    }

    private boolean hasValidMainValues(JsonNode item) {
        return isNumericValue(item, "pm10Value")
                && isNumericValue(item, "pm25Value")
                && isNumericValue(item, "o3Value")
                && isNormalFlag(item, "pm10Flag")
                && isNormalFlag(item, "pm25Flag")
                && isNormalFlag(item, "o3Flag");
    }

    private boolean isNumericValue(JsonNode item, String fieldName) {
        String value = item.path(fieldName).asString();

        if (value == null || value.isBlank() || "-".equals(value)) {
            return false;
        }

        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isNormalFlag(JsonNode item, String fieldName) {
        JsonNode node = item.path(fieldName);

        if (node.isMissingNode() || node.isNull()) {
            return true;
        }

        String value = node.asString();
        return value == null || value.isBlank();
    }

    private CurrentAirQualityObservation parseItemToObservation(JsonNode item) {
        String sidoName = item.path("sidoName").asString();
        String stationName = item.path("stationName").asString();
        String stationCode = item.path("stationCode").asString();
        String mangName = item.path("mangName").asString();

        String dataTime = item.path("dataTime").asString();

        Integer pm10Value = parseNullableInt(item, "pm10Value");
        Integer pm25Value = parseNullableInt(item, "pm25Value");
        Double o3Value = parseNullableDouble(item, "o3Value");

        Integer khaiValue = parseNullableInt(item, "khaiValue");
        Integer khaiGrade = parseNullableInt(item, "khaiGrade");

        Integer pm10Grade = parseNullableInt(item, "pm10Grade");
        Integer pm25Grade = parseNullableInt(item, "pm25Grade");
        Integer o3Grade = parseNullableInt(item, "o3Grade");

        Integer pm10Grade1h = parseNullableInt(item, "pm10Grade1h");
        Integer pm25Grade1h = parseNullableInt(item, "pm25Grade1h");

        LocalDateTime measuredAt = LocalDateTime.parse(
                dataTime,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        );

        return new CurrentAirQualityObservation(
                sidoName,
                stationName,
                stationCode,
                mangName,
                measuredAt,
                pm10Value,
                pm25Value,
                o3Value,
                khaiValue,
                khaiGrade,
                pm10Grade,
                pm25Grade,
                pm10Grade1h,
                pm25Grade1h,
                o3Grade
        );
    }

}
