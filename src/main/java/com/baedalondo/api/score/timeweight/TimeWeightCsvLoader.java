package com.baedalondo.api.score.timeweight;

import com.baedalondo.api.score.status.TimeDemandLevel;
import com.baedalondo.api.store.domain.BusinessType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 오프라인 전처리가 만든 TimeWeight CSV를 엄격하게 검증해 읽는다. */
@Component
public class TimeWeightCsvLoader {

    static final String LOCAL_RESOURCE_PATH = "time-weight/time-weight-local.csv";
    static final String CITY_RESOURCE_PATH = "time-weight/time-weight-city.csv";

    private static final List<String> LOCAL_COLUMNS = List.of(
            "commercial_area_code", "business_type", "time_band", "demand_level");
    private static final List<String> CITY_COLUMNS = List.of(
            "business_type", "time_band", "demand_level");
    private static final int EXPECTED_CITY_KEY_COUNT =
            BusinessType.values().length * TimeBand.values().length;

    public Map<LocalTimeWeightKey, TimeDemandLevel> loadLocal() {
        return loadLocal(new ClassPathResource(LOCAL_RESOURCE_PATH));
    }

    public Map<CityTimeWeightKey, TimeDemandLevel> loadCity() {
        return loadCity(new ClassPathResource(CITY_RESOURCE_PATH));
    }

    Map<LocalTimeWeightKey, TimeDemandLevel> loadLocal(Resource resource) {
        Map<LocalTimeWeightKey, TimeDemandLevel> levels = new HashMap<>();

        readRows(resource, LOCAL_COLUMNS, (fields, lineNumber) -> {
            if (fields[0].isBlank()) {
                throw fail(resource, lineNumber, "commercial_area_code가 비어 있습니다.");
            }

            LocalTimeWeightKey key = new LocalTimeWeightKey(
                    fields[0],
                    toBusinessType(resource, lineNumber, fields[1]),
                    toTimeBand(resource, lineNumber, fields[2])
            );
            TimeDemandLevel previous = levels.put(
                    key, toDemandLevel(resource, lineNumber, fields[3]));
            if (previous != null) {
                throw fail(resource, lineNumber, "중복된 Local key입니다. key=" + key);
            }
        });

        if (levels.isEmpty()) {
            throw fail(resource, 0, "Local TimeWeight가 한 건도 없습니다.");
        }
        return Map.copyOf(levels);
    }

    Map<CityTimeWeightKey, TimeDemandLevel> loadCity(Resource resource) {
        Map<CityTimeWeightKey, TimeDemandLevel> levels = new HashMap<>();

        readRows(resource, CITY_COLUMNS, (fields, lineNumber) -> {
            CityTimeWeightKey key = new CityTimeWeightKey(
                    toBusinessType(resource, lineNumber, fields[0]),
                    toTimeBand(resource, lineNumber, fields[1])
            );
            TimeDemandLevel previous = levels.put(
                    key, toDemandLevel(resource, lineNumber, fields[2]));
            if (previous != null) {
                throw fail(resource, lineNumber, "중복된 City key입니다. key=" + key);
            }
        });

        if (levels.size() != EXPECTED_CITY_KEY_COUNT) {
            throw fail(resource, 0,
                    "City TimeWeight key 개수가 올바르지 않습니다. 실제=" + levels.size()
                            + ", 기대=" + EXPECTED_CITY_KEY_COUNT);
        }
        return Map.copyOf(levels);
    }

    private void readRows(Resource resource, List<String> expectedColumns, RowHandler handler) {
        if (!resource.exists()) {
            throw fail(resource, 0, "TimeWeight CSV 리소스를 찾을 수 없습니다.");
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw fail(resource, 0, "CSV가 비어 있습니다.");
            }

            List<String> header = List.of(
                    splitStrict(resource, 1, headerLine, expectedColumns.size()));
            if (!header.equals(expectedColumns)) {
                throw fail(resource, 1,
                        "CSV 헤더가 다릅니다. 실제=" + header + ", 기대=" + expectedColumns);
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (!line.isBlank()) {
                    handler.handle(
                            splitStrict(resource, lineNumber, line, expectedColumns.size()),
                            lineNumber);
                }
            }
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "TimeWeight CSV를 읽지 못했습니다. resource=" + resource.getDescription(),
                    exception);
        }
    }

    private String[] splitStrict(Resource resource,
                                 int lineNumber,
                                 String line,
                                 int expectedFieldCount) {
        String[] fields = line.split(",", -1);
        if (fields.length != expectedFieldCount) {
            throw fail(resource, lineNumber,
                    "컬럼 수가 " + expectedFieldCount + "개가 아닙니다. 실제=" + fields.length);
        }
        for (int index = 0; index < fields.length; index++) {
            fields[index] = fields[index].trim();
        }
        return fields;
    }

    private BusinessType toBusinessType(Resource resource, int lineNumber, String value) {
        try {
            return BusinessType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw fail(resource, lineNumber, "알 수 없는 business_type입니다. value=" + value);
        }
    }

    private TimeBand toTimeBand(Resource resource, int lineNumber, String value) {
        try {
            return TimeBand.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw fail(resource, lineNumber, "알 수 없는 time_band입니다. value=" + value);
        }
    }

    private TimeDemandLevel toDemandLevel(Resource resource, int lineNumber, String value) {
        try {
            return TimeDemandLevel.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw fail(resource, lineNumber, "알 수 없는 demand_level입니다. value=" + value);
        }
    }

    private IllegalStateException fail(Resource resource, int lineNumber, String message) {
        String location = lineNumber > 0 ? " line=" + lineNumber : "";
        return new IllegalStateException(
                "TimeWeight CSV 로딩 실패. resource=" + resource.getDescription()
                        + location + " : " + message);
    }

    @FunctionalInterface
    private interface RowHandler {
        void handle(String[] fields, int lineNumber);
    }
}
