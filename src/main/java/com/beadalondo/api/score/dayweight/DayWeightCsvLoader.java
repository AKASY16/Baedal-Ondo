package com.beadalondo.api.score.dayweight;

import com.beadalondo.api.store.domain.BusinessType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 오프라인 전처리(data-processing)가 만든 DayWeight CSV를 읽는다.

 여기서는 weight를 조회만 한다. DayIndex 계산, 12분기 판단, threshold,
 clamp 같은 계산은 전부 전처리 단계에서 끝났으므로 재구현하지 않는다.

 CSV는 우리가 생성하는 기계 출력물이고 인용부호나 값 내부 쉼표가 없다.
 그래서 별도 CSV 라이브러리를 쓰지 않고 표준 라이브러리로 읽되,
 헤더와 필드 개수를 엄격히 검증해 형식이 바뀌면 조용히 잘못 읽지 않고
 애플리케이션 시작이 실패하도록 한다.
 **/
@Component
public class DayWeightCsvLoader {

    static final String LOCAL_RESOURCE_PATH = "day-weight/day-weight-local.csv";
    static final String CITY_RESOURCE_PATH = "day-weight/day-weight-city.csv";

    private static final List<String> LOCAL_COLUMNS =
            List.of("commercial_area_code", "business_type", "day_of_week", "weight");
    private static final List<String> CITY_COLUMNS =
            List.of("business_type", "day_of_week", "weight");

    private static final int MIN_WEIGHT = -6;
    private static final int MAX_WEIGHT = 6;

    // City는 지원 업종 x 요일 전체를 채우고 있어야 한다.
    private static final int EXPECTED_CITY_KEY_COUNT =
            BusinessType.values().length * DayOfWeek.values().length;

    public Map<LocalDayWeightKey, Integer> loadLocal() {
        return loadLocal(new ClassPathResource(LOCAL_RESOURCE_PATH));
    }

    public Map<CityDayWeightKey, Integer> loadCity() {
        return loadCity(new ClassPathResource(CITY_RESOURCE_PATH));
    }

    public Map<LocalDayWeightKey, Integer> loadLocal(Resource resource) {

        Map<LocalDayWeightKey, Integer> weights = new HashMap<>();

        readRows(resource, LOCAL_COLUMNS, (fields, lineNumber) -> {
            String commercialAreaCode = fields[0];

            if (commercialAreaCode.isBlank()) {
                throw fail(resource, lineNumber, "commercial_area_code가 비어 있습니다.");
            }

            LocalDayWeightKey key = new LocalDayWeightKey(
                    commercialAreaCode,
                    toBusinessType(resource, lineNumber, fields[1]),
                    toDayOfWeek(resource, lineNumber, fields[2])
            );

            Integer previous = weights.put(key, toWeight(resource, lineNumber, fields[3]));

            if (previous != null) {
                throw fail(resource, lineNumber, "중복된 Local key입니다. key=" + key);
            }
        });

        if (weights.isEmpty()) {
            throw fail(resource, 0, "Local DayWeight가 한 건도 없습니다.");
        }

        return Map.copyOf(weights);
    }

    public Map<CityDayWeightKey, Integer> loadCity(Resource resource) {

        Map<CityDayWeightKey, Integer> weights = new HashMap<>();

        readRows(resource, CITY_COLUMNS, (fields, lineNumber) -> {
            CityDayWeightKey key = new CityDayWeightKey(
                    toBusinessType(resource, lineNumber, fields[0]),
                    toDayOfWeek(resource, lineNumber, fields[1])
            );

            Integer previous = weights.put(key, toWeight(resource, lineNumber, fields[2]));

            if (previous != null) {
                throw fail(resource, lineNumber, "중복된 City key입니다. key=" + key);
            }
        });

        // 중복이 없는 상태에서 개수가 맞으면 모든 업종 x 요일 조합이 채워졌다는 뜻이다.
        if (weights.size() != EXPECTED_CITY_KEY_COUNT) {
            throw fail(resource, 0,
                    "City DayWeight key 개수가 올바르지 않습니다. 실제=" + weights.size()
                            + ", 기대=" + EXPECTED_CITY_KEY_COUNT);
        }

        return Map.copyOf(weights);
    }

    private void readRows(Resource resource, List<String> expectedColumns, RowHandler handler) {

        if (!resource.exists()) {
            throw fail(resource, 0, "DayWeight CSV 리소스를 찾을 수 없습니다.");
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();

            if (headerLine == null) {
                throw fail(resource, 0, "CSV가 비어 있습니다.");
            }

            List<String> header = List.of(splitStrict(resource, 1, headerLine, expectedColumns.size()));

            if (!header.equals(expectedColumns)) {
                throw fail(resource, 1,
                        "CSV 헤더가 다릅니다. 실제=" + header + ", 기대=" + expectedColumns);
            }

            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (line.isBlank()) {
                    continue;
                }

                handler.handle(splitStrict(resource, lineNumber, line, expectedColumns.size()), lineNumber);
            }

        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "DayWeight CSV를 읽지 못했습니다. resource=" + resource.getDescription(), e);
        }
    }

    /**
     값에 쉼표나 인용부호가 들어오면 필드 개수가 어긋나 여기서 바로 실패한다.
     */
    private String[] splitStrict(Resource resource, int lineNumber, String line, int expectedFieldCount) {

        String[] fields = line.split(",", -1);

        if (fields.length != expectedFieldCount) {
            throw fail(resource, lineNumber,
                    "컬럼 수가 " + expectedFieldCount + "개가 아닙니다. 실제=" + fields.length
                            + ", 내용=" + line);
        }

        for (int i = 0; i < fields.length; i++) {
            fields[i] = fields[i].trim();
        }

        return fields;
    }

    private BusinessType toBusinessType(Resource resource, int lineNumber, String value) {
        try {
            return BusinessType.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw fail(resource, lineNumber, "알 수 없는 business_type입니다. value=" + value);
        }
    }

    private DayOfWeek toDayOfWeek(Resource resource, int lineNumber, String value) {
        try {
            return DayOfWeek.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw fail(resource, lineNumber, "알 수 없는 day_of_week입니다. value=" + value);
        }
    }

    private int toWeight(Resource resource, int lineNumber, String value) {

        int weight;

        try {
            weight = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw fail(resource, lineNumber, "weight가 정수가 아닙니다. value=" + value);
        }

        if (weight < MIN_WEIGHT || weight > MAX_WEIGHT) {
            throw fail(resource, lineNumber,
                    "weight가 " + MIN_WEIGHT + " ~ " + MAX_WEIGHT + " 범위를 벗어났습니다. value=" + weight);
        }

        return weight;
    }

    private IllegalStateException fail(Resource resource, int lineNumber, String message) {
        String location = lineNumber > 0 ? " line=" + lineNumber : "";
        return new IllegalStateException(
                "DayWeight CSV 로딩 실패. resource=" + resource.getDescription() + location + " : " + message);
    }

    @FunctionalInterface
    private interface RowHandler {
        void handle(String[] fields, int lineNumber);
    }
}
