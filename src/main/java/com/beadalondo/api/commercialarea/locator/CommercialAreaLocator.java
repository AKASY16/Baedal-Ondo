package com.beadalondo.api.commercialarea.locator;

import com.beadalondo.api.commercialarea.domain.CommercialArea;
import com.beadalondo.api.commercialarea.dto.CommercialAreaMatch;
import com.beadalondo.api.commercialarea.loader.CommercialAreaGeoJsonLoader;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * WGS84 좌표가 어느 서울시 상권에 속하는지 판별한다.
 *
 * 상권 Geometry는 애플리케이션 시작 시 한 번만 메모리에 올린다.
 * 현재는 1,650건 선형 탐색이며, 필요해지면 이 클래스 안에서만
 * STRtree 같은 공간 인덱스로 바꾸면 된다.
 */
@Component
public class CommercialAreaLocator {

    private static final Logger log = LoggerFactory.getLogger(CommercialAreaLocator.class);

    private static final int WGS84_SRID = 4326;

    private final GeometryFactory geometryFactory =
            new GeometryFactory(new PrecisionModel(), WGS84_SRID);

    private final List<CommercialArea> commercialAreas;

    @Autowired
    public CommercialAreaLocator(CommercialAreaGeoJsonLoader loader) {
        this(loader.load());
        log.info("서울시 상권 {}건을 메모리에 로딩했습니다.", commercialAreas.size());
    }

    public CommercialAreaLocator(List<CommercialArea> commercialAreas) {
        this.commercialAreas = List.copyOf(commercialAreas);
    }

    /**
     * 좌표를 포함하는 상권을 찾는다. 어디에도 속하지 않으면 Optional.empty.
     *
     * 경계선 위의 점도 포함시키기 위해 contains가 아니라 covers를 쓴다.
     */
    public Optional<CommercialAreaMatch> find(double latitude, double longitude) {

        // GeoJSON, JTS 모두 (경도, 위도) 순서다.
        Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));

        List<CommercialArea> matched = new ArrayList<>();

        for (CommercialArea area : commercialAreas) {
            Geometry geometry = area.getGeometry();

            // 값비싼 covers 전에 바운딩 박스로 먼저 걸러낸다.
            if (!geometry.getEnvelopeInternal().covers(point.getCoordinate())) {
                continue;
            }

            if (geometry.covers(point)) {
                matched.add(area);
            }
        }

        if (matched.isEmpty()) {
            return Optional.empty();
        }

        if (matched.size() == 1) {
            return Optional.of(matched.get(0).toMatch());
        }

        return Optional.of(resolveOverlap(latitude, longitude, matched).toMatch());
    }

    public int size() {
        return commercialAreas.size();
    }

    /**
     * 한 좌표가 둘 이상의 상권에 포함되는 비정상 상황 처리.
     *
     * 현재 데이터에서는 발생하지 않을 것으로 보지만, 발생하면
     * 매장 등록을 실패시키지 않고 가장 좁은 상권(= 가장 구체적인 상권)을 고른다.
     * 면적이 같으면 상권코드 오름차순으로 정해 결과를 결정적으로 만든다.
     * 데이터 문제를 놓치지 않도록 WARN 로그를 남긴다.
     */
    private CommercialArea resolveOverlap(double latitude,
                                          double longitude,
                                          List<CommercialArea> matched) {

        CommercialArea selected = matched.stream()
                .min(Comparator
                        .comparingDouble((CommercialArea area) -> area.getGeometry().getArea())
                        .thenComparing(CommercialArea::getCode))
                .orElseThrow();

        log.warn("좌표가 {}개 상권에 중복 포함되었습니다. latitude={}, longitude={}, 후보={}, 선택={}",
                matched.size(),
                latitude,
                longitude,
                matched.stream().map(CommercialArea::getCode).toList(),
                selected.getCode());

        return selected;
    }
}
