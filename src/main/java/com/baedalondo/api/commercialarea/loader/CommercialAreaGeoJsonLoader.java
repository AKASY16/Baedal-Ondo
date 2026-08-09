package com.baedalondo.api.commercialarea.loader;

import com.baedalondo.api.commercialarea.domain.CommercialArea;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 서울시 상권 GeoJSON을 읽어 JTS Geometry 목록으로 변환한다.
 파일이 6MB 이상이라 전체 트리를 한 번에 올리지 않고
 features 배열을 스트리밍으로 한 건씩 읽는다.
 */
@Component
public class CommercialAreaGeoJsonLoader {

    private static final String GEOJSON_PATH = "commercial-area/seoul-commercial-areas.geojson";

    private static final int WGS84_SRID = 4326;

    private static final String PROPERTY_TYPE_CODE = "TRDAR_SE_CD";
    private static final String PROPERTY_TYPE_NAME = "TRDAR_SE_CD_NM";
    private static final String PROPERTY_CODE = "TRDAR_CD";
    private static final String PROPERTY_NAME = "TRDAR_CD_NM";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final GeometryFactory geometryFactory =
            new GeometryFactory(new PrecisionModel(), WGS84_SRID);

    /**
     * GeoJSON 리소스를 읽어 상권 목록을 만든다.
     *
     * 실패하면 빈 목록으로 넘어가지 않고 예외를 던진다.
     */
    public List<CommercialArea> load() {

        ClassPathResource resource = new ClassPathResource(GEOJSON_PATH);

        if (!resource.exists()) {
            throw new IllegalStateException(
                    "상권 GeoJSON 리소스를 찾을 수 없습니다. path=" + GEOJSON_PATH);
        }

        List<CommercialArea> areas;

        try (InputStream inputStream = resource.getInputStream();
             JsonParser parser = objectMapper.createParser(inputStream)) {

            areas = readFeatureCollection(parser);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "상권 GeoJSON 로딩에 실패했습니다. path=" + GEOJSON_PATH, e);
        }

        if (areas.isEmpty()) {
            throw new IllegalStateException(
                    "상권 GeoJSON에서 읽어 들인 상권이 없습니다. path=" + GEOJSON_PATH);
        }

        return areas;
    }

    private List<CommercialArea> readFeatureCollection(JsonParser parser) {

        List<CommercialArea> areas = new ArrayList<>();

        if (parser.nextToken() != JsonToken.START_OBJECT) {
            throw new IllegalStateException("GeoJSON 최상위가 객체가 아닙니다.");
        }

        boolean featuresFound = false;

        while (parser.nextToken() != JsonToken.END_OBJECT) {

            String fieldName = parser.currentName();
            parser.nextToken();

            if (!"features".equals(fieldName)) {
                parser.skipChildren();
                continue;
            }

            if (parser.currentToken() != JsonToken.START_ARRAY) {
                throw new IllegalStateException("GeoJSON features가 배열이 아닙니다.");
            }

            featuresFound = true;

            while (parser.nextToken() != JsonToken.END_ARRAY) {
                // feature 하나만 읽는다. readTree는 뒤에 남은 토큰을 오류로 보기 때문에
                // 배열을 순회할 때는 쓸 수 없다.
                JsonNode feature = parser.readValueAsTree();
                areas.add(toCommercialArea(feature));
            }
        }

        if (!featuresFound) {
            throw new IllegalStateException("GeoJSON에 features 필드가 없습니다.");
        }

        return areas;
    }

    private CommercialArea toCommercialArea(JsonNode feature) {

        JsonNode properties = feature.path("properties");

        String code = properties.path(PROPERTY_CODE).asString(null);

        if (code == null || code.isBlank()) {
            throw new IllegalStateException(
                    "상권코드(" + PROPERTY_CODE + ")가 없는 feature가 있습니다.");
        }

        Geometry geometry = toGeometry(code, feature.path("geometry"));

        return new CommercialArea(
                code,
                properties.path(PROPERTY_NAME).asString(null),
                properties.path(PROPERTY_TYPE_CODE).asString(null),
                properties.path(PROPERTY_TYPE_NAME).asString(null),
                geometry
        );
    }

    private Geometry toGeometry(String code, JsonNode geometry) {

        String type = geometry.path("type").asString(null);
        JsonNode coordinates = geometry.path("coordinates");

        if (type == null || !coordinates.isArray()) {
            throw new IllegalStateException(
                    "geometry가 올바르지 않습니다. 상권코드=" + code);
        }

        return switch (type) {
            case "Polygon" -> toPolygon(coordinates);
            case "MultiPolygon" -> toMultiPolygon(coordinates);
            default -> throw new IllegalStateException(
                    "지원하지 않는 geometry 타입입니다. type=" + type + ", 상권코드=" + code);
        };
    }

    private Geometry toMultiPolygon(JsonNode polygons) {

        Polygon[] converted = new Polygon[polygons.size()];

        for (int i = 0; i < polygons.size(); i++) {
            converted[i] = toPolygon(polygons.get(i));
        }

        return geometryFactory.createMultiPolygon(converted);
    }

    private Polygon toPolygon(JsonNode rings) {

        LinearRing shell = toLinearRing(rings.get(0));

        LinearRing[] holes = new LinearRing[rings.size() - 1];

        for (int i = 1; i < rings.size(); i++) {
            holes[i - 1] = toLinearRing(rings.get(i));
        }

        return geometryFactory.createPolygon(shell, holes);
    }

    private LinearRing toLinearRing(JsonNode ring) {

        Coordinate[] coordinates = new Coordinate[ring.size()];

        for (int i = 0; i < ring.size(); i++) {
            JsonNode point = ring.get(i);

            // GeoJSON 좌표 순서는 [경도, 위도] 이고
            // JTS Coordinate(x, y)도 (경도, 위도) 순서다.
            coordinates[i] = new Coordinate(
                    point.get(0).asDouble(),
                    point.get(1).asDouble()
            );
        }

        return geometryFactory.createLinearRing(coordinates);
    }
}
