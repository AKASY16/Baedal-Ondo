package com.baedalondo.api.guest.service;

import com.baedalondo.api.guest.domain.GuestRegion;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 고정 게스트 지역 CSV를 검증해 메모리 데이터로 읽는다. */
@Component
public class GuestRegionCsvLoader {

    static final String RESOURCE_PATH = "guest-regions.csv";

    private static final List<String> EXPECTED_COLUMNS = List.of(
            "id", "display_name", "address", "road_address", "jibun_address", "address_detail",
            "postal_code", "sido_name", "sigungu_name", "dong_name",
            "address_region_code", "road_name_code", "building_management_number",
            "road_name", "underground_yn", "building_main_number",
            "building_sub_number", "nx", "ny"
    );

    public List<GuestRegion> load() {
        return load(new ClassPathResource(RESOURCE_PATH));
    }

    List<GuestRegion> load(Resource resource) {
        if (!resource.exists()) {
            throw fail(resource, 0, "CSV 리소스를 찾을 수 없습니다.");
        }

        List<GuestRegion> regions = new ArrayList<>();
        Set<Long> ids = new HashSet<>();
        Set<String> districts = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw fail(resource, 0, "CSV가 비어 있습니다.");
            }

            List<String> header = parseLine(headerLine);
            if (!header.equals(EXPECTED_COLUMNS)) {
                throw fail(resource, 1,
                        "CSV 헤더가 올바르지 않습니다. actual=" + header + ", expected=" + EXPECTED_COLUMNS);
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }

                List<String> fields = parseLine(line);
                if (fields.size() != EXPECTED_COLUMNS.size()) {
                    throw fail(resource, lineNumber,
                            "필드 수가 올바르지 않습니다. actual=" + fields.size()
                                    + ", expected=" + EXPECTED_COLUMNS.size());
                }

                GuestRegion region = toRegion(resource, lineNumber, fields);
                if (!ids.add(region.getId())) {
                    throw fail(resource, lineNumber, "중복된 id입니다. id=" + region.getId());
                }
                if (!districts.add(region.getSigunguName())) {
                    throw fail(resource, lineNumber,
                            "중복된 자치구입니다. sigunguName=" + region.getSigunguName());
                }
                regions.add(region);
            }
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "게스트 지역 CSV를 읽지 못했습니다. resource=" + resource.getDescription(),
                    exception
            );
        }

        if (regions.isEmpty()) {
            throw fail(resource, 0, "게스트 지역이 한 건도 없습니다.");
        }

        return List.copyOf(regions);
    }

    private GuestRegion toRegion(Resource resource, int lineNumber, List<String> fields) {
        long id = parseLong(resource, lineNumber, fields.get(0), "id");
        int nx = parseInt(resource, lineNumber, fields.get(17), "nx");
        int ny = parseInt(resource, lineNumber, fields.get(18), "ny");

        requireText(resource, lineNumber, fields.get(1), "display_name");
        requireText(resource, lineNumber, fields.get(2), "address");
        requireText(resource, lineNumber, fields.get(3), "road_address");
        requireText(resource, lineNumber, fields.get(7), "sido_name");
        requireText(resource, lineNumber, fields.get(8), "sigungu_name");

        return new GuestRegion(
                id, fields.get(1), fields.get(2), fields.get(3), fields.get(4),
                fields.get(5), fields.get(6), fields.get(7), fields.get(8),
                fields.get(9), fields.get(10), fields.get(11), fields.get(12),
                fields.get(13), fields.get(14), fields.get(15), fields.get(16), nx, ny
        );
    }

    private List<String> parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;

        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);

            if (current == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    field.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ',' && !quoted) {
                fields.add(field.toString().trim());
                field.setLength(0);
            } else {
                field.append(current);
            }
        }

        if (quoted) {
            throw new IllegalStateException("닫히지 않은 CSV 따옴표가 있습니다.");
        }

        fields.add(field.toString().trim());
        return fields;
    }

    private long parseLong(Resource resource, int lineNumber, String value, String fieldName) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw fail(resource, lineNumber, fieldName + "이 숫자가 아닙니다. value=" + value);
        }
    }

    private int parseInt(Resource resource, int lineNumber, String value, String fieldName) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw fail(resource, lineNumber, fieldName + "가 숫자가 아닙니다. value=" + value);
        }
    }

    private void requireText(Resource resource, int lineNumber, String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw fail(resource, lineNumber, fieldName + "가 비어 있습니다.");
        }
    }

    private IllegalStateException fail(Resource resource, int lineNumber, String message) {
        String location = lineNumber > 0 ? " line=" + lineNumber : "";
        return new IllegalStateException(
                "게스트 지역 CSV 로딩 실패. resource=" + resource.getDescription()
                        + location + " : " + message
        );
    }
}
