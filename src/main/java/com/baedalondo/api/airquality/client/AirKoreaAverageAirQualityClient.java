package com.baedalondo.api.airquality.client;

import com.baedalondo.api.airquality.domain.CurrentAirQualityObservation;
import com.baedalondo.api.airquality.exception.AirKoreaApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AirKoreaAverageAirQualityClient {

    private static final DateTimeFormatter DATA_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final RestClient restClient;
    private final String authKey;
    private final Map<String, CachedAverage> cachedAverages =
            new ConcurrentHashMap<>();

    public AirKoreaAverageAirQualityClient(
            RestClient.Builder restClientBuilder,
            @Value("${dataportal.api.air-quality-stats-base-url}") String baseUrl,
            @Value("${dataportal.api.auth-key}") String authKey
    ) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
        this.authKey = authKey;
    }

    public CurrentAirQualityObservation getHourlyAverage(
            String sidoName,
            LocalDateTime baseTime
    ) {
        CachedAverage cachedAverage = cachedAverages.get(sidoName);

        if (cachedAverage != null
                && Objects.equals(cachedAverage.baseTime(), baseTime)) {
            return cachedAverage.observation();
        }

        CurrentAirQualityObservation observation =
                requestHourlyAverage(sidoName, baseTime);

        cachedAverages.put(
                sidoName,
                new CachedAverage(baseTime, observation)
        );

        return observation;
    }

    private CurrentAirQualityObservation requestHourlyAverage(
            String sidoName,
            LocalDateTime baseTime
    ) {
        try {
            Map<LocalDateTime, Integer> pm10Values =
                    getHourlyValues("PM10", sidoName);
            Map<LocalDateTime, Integer> pm25Values =
                    getHourlyValues("PM25", sidoName);

            LocalDateTime measuredAt = pm10Values.keySet().stream()
                    .filter(pm25Values::containsKey)
                    .filter(dataTime -> baseTime == null || !dataTime.isAfter(baseTime))
                    .max(LocalDateTime::compareTo)
                    .orElseThrow(() -> new AirKoreaApiException(
                            "PM10과 PM2.5의 공통 시도 평균 측정 시각이 없습니다."
                    ));

            return new CurrentAirQualityObservation(
                    sidoName,
                    sidoName + " 평균",
                    null,
                    "시도 평균",
                    measuredAt,
                    pm10Values.get(measuredAt),
                    pm25Values.get(measuredAt),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        } catch (AirKoreaApiException e) {
            throw e;
        } catch (RestClientException | NumberFormatException | DateTimeException e) {
            throw new AirKoreaApiException(
                    "에어코리아 시도 평균 API 호출 또는 응답 처리 중 오류가 발생했습니다.",
                    e
            );
        }
    }

    private Map<LocalDateTime, Integer> getHourlyValues(
            String itemCode,
            String sidoName
    ) {
        JsonNode root = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/getCtprvnMesureLIst")
                        .queryParam("itemCode", itemCode)
                        .queryParam("dataGubun", "HOUR")
                        .queryParam("pageNo", 1)
                        .queryParam("numOfRows", 100)
                        .queryParam("returnType", "json")
                        .queryParam("serviceKey", authKey)
                        .build())
                .retrieve()
                .body(JsonNode.class);

        return parseHourlyValues(root, sidoName);
    }

    Map<LocalDateTime, Integer> parseHourlyValues(
            JsonNode root,
            String sidoName
    ) {
        if (root == null) {
            throw new AirKoreaApiException("에어코리아 시도 평균 API 응답이 비어 있습니다.");
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
                    "에어코리아 시도 평균 API 오류, resultCode="
                            + resultCode + ", resultMsg=" + resultMsg
            );
        }

        JsonNode items = root.path("response")
                .path("body")
                .path("items");

        if (!items.isArray() || items.isEmpty()) {
            throw new AirKoreaApiException("에어코리아 시도 평균 API 응답에 데이터가 없습니다.");
        }

        String sidoFieldName = toResponseFieldName(sidoName);
        Map<LocalDateTime, Integer> values = new HashMap<>();

        for (JsonNode item : items) {
            Integer value = parseNullableAverage(item, sidoFieldName);
            String dataTime = item.path("dataTime").asString();

            if (value == null || dataTime == null || dataTime.isBlank()) {
                continue;
            }

            values.put(
                    LocalDateTime.parse(dataTime, DATA_TIME_FORMATTER),
                    value
            );
        }

        if (values.isEmpty()) {
            throw new AirKoreaApiException(
                    "사용 가능한 " + sidoName + " 시도 평균 데이터가 없습니다."
            );
        }

        return values;
    }

    private Integer parseNullableAverage(JsonNode item, String fieldName) {
        String value = item.path(fieldName).asString();

        if (value == null || value.isBlank() || "-".equals(value)) {
            return null;
        }

        return (int) Math.round(Double.parseDouble(value));
    }

    private String toResponseFieldName(String sidoName) {
        return switch (sidoName) {
            case "서울" -> "seoul";
            case "부산" -> "busan";
            case "대구" -> "daegu";
            case "인천" -> "incheon";
            case "광주" -> "gwangju";
            case "대전" -> "daejeon";
            case "울산" -> "ulsan";
            case "세종" -> "sejong";
            case "경기" -> "gyeonggi";
            case "강원" -> "gangwon";
            case "충북" -> "chungbuk";
            case "충남" -> "chungnam";
            case "전북" -> "jeonbuk";
            case "전남" -> "jeonnam";
            case "경북" -> "gyeongbuk";
            case "경남" -> "gyeongnam";
            case "제주" -> "jeju";
            default -> throw new AirKoreaApiException(
                    "지원하지 않는 시도 평균 조회 지역입니다: " + sidoName
            );
        };
    }

    private record CachedAverage(
            LocalDateTime baseTime,
            CurrentAirQualityObservation observation
    ) {
    }
}
