package com.baedalondo.api.weather.client;

import com.baedalondo.api.weather.domain.ForecastWeatherObservation;
import com.baedalondo.api.weather.exception.KmaWeatherApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


@Component
public class KmaForecastWeatherClient {

    private static final DateTimeFormatter FORECAST_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final RestClient restClient;
    private final String authKey;


    public KmaForecastWeatherClient(
            RestClient.Builder restClientBuilder,
            @Value("${kma.api.base-url}") String baseUrl,
            @Value("${kma.api.auth-key}") String authKey
    ) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();

        this.authKey = authKey;
    }


    public List<ForecastWeatherObservation> getForecastWeather(
            int nx,
            int ny,
            String baseDate,
            String baseTime
    ) {
        try {
            JsonNode root = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/getUltraSrtFcst")
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

            return parseForecastWeather(root);

        } catch (RestClientException | NumberFormatException e) {
            throw new KmaWeatherApiException(
                    "기상청 API 호출 또는 응답 처리 중 오류가 발생했습니다.",
                    e
            );
        }
    }


    private List<ForecastWeatherObservation> parseForecastWeather(JsonNode root) {

        validateResponse(root);


        //items 변수에 응답값이 들어감.
        JsonNode items = root.path("response")
                .path("body")
                .path("items")
                .path("item");

        //forecastMap 변수에 map 형태로 들어감
        Map<LocalDateTime, Map<String, String>> forecastMap =
                //아까 응답값이 들어간 items변수를 spliterator로 순회 가능한 형태로 만듬
                StreamSupport.stream(items.spliterator(), false)
                        //까본걸 collect로 수집함
                        .collect(
                                //collect된걸 묶음
                                Collectors.groupingBy(
                                        //키로 쓰는건 extractForecastAt메서드로 나온 값을 써라
                                        this::extractForecastAt,
                                        //바깥 Map의 구현체를 LinkedHashMap으로 사용
                                        LinkedHashMap::new,
                                        //values는 map으로 하는데, 아까 item에서 category랑 fcstValue 값을 빼서 둘이 맵으로 묶어서 해라
                                        Collectors.toMap(
                                                //안쪽 Map의 키값
                                                item -> item.path("category").asString(),
                                                //안쪽 Map의 Value값
                                                item -> item.path("fcstValue").asString(),
                                                (existingValue, newValue) -> newValue,
                                                LinkedHashMap::new
                                        )
                                )
                        );


        return forecastMap.entrySet()
                .stream()
                .map(entry -> {

                    LocalDateTime forecastAt = entry.getKey();
                    Map<String, String> values = entry.getValue();

                    validateRequiredCategories(
                            forecastAt,
                            values
                    );

                    int precipitationType =
                            Integer.parseInt(values.get("PTY"));

                    double rainfall =
                            parseRainfall(values.get("RN1"));

                    double temperature =
                            Double.parseDouble(values.get("T1H"));

                    int humidity =
                            Integer.parseInt(values.get("REH"));

                    double windSpeed =
                            Double.parseDouble(values.get("WSD"));


                    return new ForecastWeatherObservation(
                            forecastAt,
                            precipitationType,
                            rainfall,
                            temperature,
                            humidity,
                            windSpeed
                    );
                })
                .toList();
    }


    private LocalDateTime extractForecastAt(JsonNode item) {

        String forecastDate =
                item.path("fcstDate").asString();

        String forecastTime =
                item.path("fcstTime").asString();

        return LocalDateTime.parse(
                forecastDate + forecastTime,
                FORECAST_DATE_TIME_FORMATTER
        );
    }


    private void validateRequiredCategories(
            LocalDateTime forecastAt,
            Map<String, String> values
    ) {

        if (!values.containsKey("PTY")
                || !values.containsKey("RN1")
                || !values.containsKey("T1H")
                || !values.containsKey("REH")
                || !values.containsKey("WSD")) {

            throw new KmaWeatherApiException(
                    forecastAt + " 예보에 필수 날씨 항목이 누락되었습니다."
            );
        }
    }


    /**
     * 초단기예보의 RN1은 실황과 달리 문자열 구간으로 내려온다.
     *
     *   강수없음        -> 0.0
     *   1.0mm 미만      -> 0.5   (0.1 ~ 1.0 구간의 대푯값)
     *   3.5mm           -> 3.5
     *   30.0~50.0mm     -> 30.0  (구간 하한. 점수 임계값이 30이라 하한이 곧 해당 등급이다)
     *   50.0mm 이상     -> 50.0
     *
     * 구간 값은 대푯값이므로 정확한 강수량이 아니라 점수 구간 판정을 위한 값이다.
     */
    private double parseRainfall(String value) {

        if (value == null || value.isBlank() || "강수없음".equals(value.trim())) {
            return 0.0;
        }

        String normalized = value.trim();

        if (normalized.startsWith("1.0mm 미만") || normalized.startsWith("1mm 미만")) {
            return 0.5;
        }

        // "30.0~50.0mm" 형태는 하한을 사용한다.
        int rangeIndex = normalized.indexOf('~');
        if (rangeIndex > 0) {
            return parseNumericPart(normalized.substring(0, rangeIndex), value);
        }

        // "50.0mm 이상"과 "3.5mm"는 모두 앞쪽 숫자를 그대로 쓴다.
        return parseNumericPart(normalized, value);
    }

    private double parseNumericPart(String text, String originalValue) {

        String digits = text.replaceAll("[^0-9.]", "");

        if (digits.isBlank()) {
            throw new KmaWeatherApiException(
                    "처리할 수 없는 강수량 값입니다: " + originalValue
            );
        }

        try {
            return Double.parseDouble(digits);
        } catch (NumberFormatException exception) {
            throw new KmaWeatherApiException(
                    "처리할 수 없는 강수량 값입니다: " + originalValue,
                    exception
            );
        }
    }


    private void validateResponse(JsonNode root) {

        if (root == null) {
            throw new KmaWeatherApiException(
                    "기상청 API 응답이 비어있습니다."
            );
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

            throw new KmaWeatherApiException(
                    "기상청 API 에러, 코드: " + resultMsg
            );
        }


        JsonNode items = root.path("response")
                .path("body")
                .path("items")
                .path("item");

        if (!items.isArray() || items.isEmpty()) {
            throw new KmaWeatherApiException(
                    "기상청 API 응답에 날씨 예보 항목이 없습니다."
            );
        }
    }
}