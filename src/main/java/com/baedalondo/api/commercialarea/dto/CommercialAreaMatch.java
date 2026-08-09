package com.baedalondo.api.commercialarea.dto;

/**
 좌표 1건에 대한 상권 판별 결과.
 Geometry는 담지 않는다. 저장 대상이 되는 값만 노출한다.
 **/
public class CommercialAreaMatch {

    private final String commercialAreaCode;
    private final String commercialAreaName;
    private final String commercialAreaTypeCode;
    private final String commercialAreaTypeName;

    public CommercialAreaMatch(String commercialAreaCode,
                               String commercialAreaName,
                               String commercialAreaTypeCode,
                               String commercialAreaTypeName) {
        this.commercialAreaCode = commercialAreaCode;
        this.commercialAreaName = commercialAreaName;
        this.commercialAreaTypeCode = commercialAreaTypeCode;
        this.commercialAreaTypeName = commercialAreaTypeName;
    }

    public String getCommercialAreaCode() {
        return commercialAreaCode;
    }

    public String getCommercialAreaName() {
        return commercialAreaName;
    }

    public String getCommercialAreaTypeCode() {
        return commercialAreaTypeCode;
    }

    public String getCommercialAreaTypeName() {
        return commercialAreaTypeName;
    }
}
