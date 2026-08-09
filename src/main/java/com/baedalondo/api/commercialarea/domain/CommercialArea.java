package com.baedalondo.api.commercialarea.domain;

import com.baedalondo.api.commercialarea.dto.CommercialAreaMatch;
import org.locationtech.jts.geom.Geometry;

/**
 메모리에 상주하는 서울시 상권 1건.
 GeoJSON에서 읽어 들인 읽기 전용 값.
 **/
public class CommercialArea {

    private final String code;       // TRDAR_CD, 상권코드
    private final String name;       // TRDAR_CD_NM, 상권명
    private final String typeCode;   // TRDAR_SE_CD, 상권 구분 코드
    private final String typeName;   // TRDAR_SE_CD_NM, 상권 구분명

    private final Geometry geometry; // EPSG:4326, Polygon 또는 MultiPolygon

    public CommercialArea(String code,
                          String name,
                          String typeCode,
                          String typeName,
                          Geometry geometry) {
        this.code = code;
        this.name = name;
        this.typeCode = typeCode;
        this.typeName = typeName;
        this.geometry = geometry;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public String getTypeName() {
        return typeName;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public CommercialAreaMatch toMatch() {
        return new CommercialAreaMatch(code, name, typeCode, typeName);
    }
}
