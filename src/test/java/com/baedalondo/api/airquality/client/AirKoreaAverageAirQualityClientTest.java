package com.baedalondo.api.airquality.client;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AirKoreaAverageAirQualityClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AirKoreaAverageAirQualityClient client =
            new AirKoreaAverageAirQualityClient(
                    RestClient.builder(),
                    "https://apis.data.go.kr/B552584/ArpltnStatsSvc",
                    "test-key"
            );

    @Test
    void parseHourlyValuesReadsSeoulAverageAndRoundsDecimalValue() throws Exception {
        JsonNode root = objectMapper.readTree("""
                {
                  "response": {
                    "header": {
                      "resultCode": "00",
                      "resultMsg": "NORMAL_CODE"
                    },
                    "body": {
                      "items": [
                        {
                          "dataTime": "2026-08-14 22:00",
                          "dataGubun": "시간평균",
                          "seoul": "21.6"
                        },
                        {
                          "dataTime": "2026-08-14 21:00",
                          "dataGubun": "시간평균",
                          "seoul": "-"
                        }
                      ]
                    }
                  }
                }
                """);

        Map<LocalDateTime, Integer> result =
                client.parseHourlyValues(root, "서울");

        assertEquals(1, result.size());
        assertEquals(
                22,
                result.get(LocalDateTime.of(2026, 8, 14, 22, 0))
        );
    }
}
