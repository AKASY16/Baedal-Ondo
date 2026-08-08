package com.beadalondo.api.commercialarea.loader;

import com.beadalondo.api.commercialarea.domain.CommercialArea;
import com.beadalondo.api.commercialarea.dto.CommercialAreaMatch;
import com.beadalondo.api.commercialarea.locator.CommercialAreaLocator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 실제 서울시 상권 GeoJSON 리소스를 대상으로 하는 테스트.
 */
class CommercialAreaGeoJsonLoaderTest {

    private static List<CommercialArea> commercialAreas;
    private static CommercialAreaLocator locator;

    @BeforeAll
    static void loadOnce() {
        commercialAreas = new CommercialAreaGeoJsonLoader().load();
        locator = new CommercialAreaLocator(commercialAreas);
    }

    @Test
    @DisplayName("서울시 상권 1,650건이 모두 로딩된다")
    void loadsAllAreas() {
        assertEquals(1650, commercialAreas.size());
    }

    @Test
    @DisplayName("모든 상권이 상권코드와 EPSG:4326 geometry를 가진다")
    void everyAreaHasCodeAndGeometry() {

        for (CommercialArea area : commercialAreas) {
            assertNotNull(area.getCode());
            assertFalse(area.getCode().isBlank());
            assertNotNull(area.getGeometry());
            assertEquals(4326, area.getGeometry().getSRID());
            assertFalse(area.getGeometry().isEmpty());
        }
    }

    @Test
    @DisplayName("상권코드는 중복되지 않는다")
    void codesAreUnique() {

        Set<String> codes = new HashSet<>();

        for (CommercialArea area : commercialAreas) {
            assertTrue(codes.add(area.getCode()),
                    "상권코드가 중복되었습니다. code=" + area.getCode());
        }
    }

    @Test
    @DisplayName("Polygon과 MultiPolygon이 모두 로딩된다")
    void loadsBothGeometryTypes() {

        long polygonCount = commercialAreas.stream()
                .filter(area -> area.getGeometry() instanceof Polygon)
                .count();

        long multiPolygonCount = commercialAreas.stream()
                .filter(area -> area.getGeometry() instanceof MultiPolygon)
                .count();

        assertEquals(1561, polygonCount);
        assertEquals(89, multiPolygonCount);
    }

    @Test
    @DisplayName("Polygon 상권 내부 좌표는 그 상권코드로 판별된다")
    void findsPolygonArea() {
        assertInteriorPointResolvesToItsOwnArea(firstAreaOfType(Polygon.class));
    }

    @Test
    @DisplayName("MultiPolygon 상권 내부 좌표는 그 상권코드로 판별된다")
    void findsMultiPolygonArea() {
        assertInteriorPointResolvesToItsOwnArea(firstAreaOfType(MultiPolygon.class));
    }

    @Test
    @DisplayName("실제 상권 경계선 위 좌표도 covers 기준으로 포함된다")
    void findsBoundaryCoordinate() {

        CommercialArea area = firstAreaOfType(Polygon.class);

        Coordinate vertex = area.getGeometry().getCoordinates()[0];

        Optional<CommercialAreaMatch> match = locator.find(vertex.y, vertex.x);

        assertTrue(match.isPresent(), "경계 꼭짓점 좌표가 어떤 상권에도 포함되지 않았습니다.");
    }

    @Test
    @DisplayName("서울시 상권 밖 좌표는 Optional.empty를 반환한다")
    void returnsEmptyOutsideSeoul() {

        // 제주도
        assertTrue(locator.find(33.4996, 126.5312).isEmpty());

        // 서해 앞바다
        assertTrue(locator.find(37.0000, 125.0000).isEmpty());
    }

    private CommercialArea firstAreaOfType(Class<? extends Geometry> type) {
        return commercialAreas.stream()
                .filter(area -> type.isInstance(area.getGeometry()))
                .findFirst()
                .orElseThrow();
    }

    private void assertInteriorPointResolvesToItsOwnArea(CommercialArea area) {

        Point interiorPoint = area.getGeometry().getInteriorPoint();

        Optional<CommercialAreaMatch> match =
                locator.find(interiorPoint.getY(), interiorPoint.getX());

        assertTrue(match.isPresent(),
                "상권 내부 좌표가 판별되지 않았습니다. code=" + area.getCode());
        assertEquals(area.getCode(), match.get().getCommercialAreaCode());
    }
}
