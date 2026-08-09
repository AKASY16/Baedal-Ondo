package com.baedalondo.api.commercialarea.locator;

import com.baedalondo.api.commercialarea.domain.CommercialArea;
import com.baedalondo.api.commercialarea.dto.CommercialAreaMatch;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommercialAreaLocatorTest {

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    // 경도 127.0 ~ 127.1, 위도 37.0 ~ 37.1 사각형
    private final CommercialArea polygonArea = new CommercialArea(
            "1000001", "폴리곤상권", "A", "골목상권",
            square(127.0, 37.0, 127.1, 37.1));

    // 서로 떨어진 두 사각형
    private final CommercialArea multiPolygonArea = new CommercialArea(
            "1000002", "멀티폴리곤상권", "D", "발달상권",
            GEOMETRY_FACTORY.createMultiPolygon(new Polygon[]{
                    square(128.0, 37.0, 128.1, 37.1),
                    square(128.2, 37.0, 128.3, 37.1)
            }));

    private final CommercialAreaLocator locator =
            new CommercialAreaLocator(List.of(polygonArea, multiPolygonArea));

    @Test
    @DisplayName("상권 내부 좌표는 해당 상권코드를 반환한다")
    void findInsidePolygon() {

        Optional<CommercialAreaMatch> match = locator.find(37.05, 127.05);

        assertTrue(match.isPresent());
        assertEquals("1000001", match.get().getCommercialAreaCode());
        assertEquals("폴리곤상권", match.get().getCommercialAreaName());
        assertEquals("A", match.get().getCommercialAreaTypeCode());
        assertEquals("골목상권", match.get().getCommercialAreaTypeName());
    }

    @Test
    @DisplayName("어느 상권에도 속하지 않는 좌표는 Optional.empty를 반환한다")
    void findOutsideAllAreas() {

        assertTrue(locator.find(37.05, 126.0).isEmpty());
        assertTrue(locator.find(38.0, 127.05).isEmpty());

        // MultiPolygon 조각 사이의 빈 공간
        assertTrue(locator.find(37.05, 128.15).isEmpty());
    }

    @Test
    @DisplayName("MultiPolygon은 모든 조각에 대해 판별된다")
    void findInsideMultiPolygon() {

        Optional<CommercialAreaMatch> firstPart = locator.find(37.05, 128.05);
        Optional<CommercialAreaMatch> secondPart = locator.find(37.05, 128.25);

        assertTrue(firstPart.isPresent());
        assertTrue(secondPart.isPresent());
        assertEquals("1000002", firstPart.get().getCommercialAreaCode());
        assertEquals("1000002", secondPart.get().getCommercialAreaCode());
    }

    @Test
    @DisplayName("경계선 위 좌표도 covers 기준으로 상권에 포함된다")
    void findOnBoundary() {

        // 변 위의 점
        Optional<CommercialAreaMatch> onEdge = locator.find(37.05, 127.0);

        // 꼭짓점
        Optional<CommercialAreaMatch> onVertex = locator.find(37.1, 127.1);

        assertTrue(onEdge.isPresent(), "경계선 위 좌표가 포함되지 않았습니다.");
        assertTrue(onVertex.isPresent(), "꼭짓점 좌표가 포함되지 않았습니다.");
        assertEquals("1000001", onEdge.get().getCommercialAreaCode());
        assertEquals("1000001", onVertex.get().getCommercialAreaCode());
    }

    @Test
    @DisplayName("위도와 경도를 뒤집어 넣으면 판별되지 않는다, 좌표 순서 회귀 방지")
    void coordinateOrderMatters() {

        // find(latitude, longitude) 이므로 뒤집으면 서울 밖 좌표가 된다.
        assertTrue(locator.find(127.05, 37.05).isEmpty());
    }

    @Test
    @DisplayName("좌표가 두 상권에 중복 포함되면 더 좁은 상권을 고른다")
    void resolveOverlappingAreas() {

        CommercialArea wide = new CommercialArea(
                "2000001", "넓은상권", "D", "발달상권",
                square(127.0, 37.0, 127.5, 37.5));

        CommercialArea narrow = new CommercialArea(
                "2000002", "좁은상권", "A", "골목상권",
                square(127.0, 37.0, 127.1, 37.1));

        CommercialAreaLocator overlapping =
                new CommercialAreaLocator(List.of(wide, narrow));

        Optional<CommercialAreaMatch> match = overlapping.find(37.05, 127.05);

        assertTrue(match.isPresent());
        assertEquals("2000002", match.get().getCommercialAreaCode());
    }

    private static Polygon square(double minLon, double minLat, double maxLon, double maxLat) {

        Coordinate[] coordinates = {
                new Coordinate(minLon, minLat),
                new Coordinate(maxLon, minLat),
                new Coordinate(maxLon, maxLat),
                new Coordinate(minLon, maxLat),
                new Coordinate(minLon, minLat)
        };

        return GEOMETRY_FACTORY.createPolygon(coordinates);
    }
}
