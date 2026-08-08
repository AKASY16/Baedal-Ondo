package com.beadalondo.api.score.dto;

import com.beadalondo.api.guest.domain.GuestRegion;
import com.beadalondo.api.store.domain.BusinessType;
import com.beadalondo.api.store.domain.Store;

public class ScoreTarget {

    private final Long id;
    private final String sidoName;
    private final String sigunguName;
    private final Integer nx;
    private final Integer ny;

    // 상권별 요일 가중치 조회용. 상권 밖 매장이나 게스트 지역이면 null이다.
    private final String commercialAreaCode;
    private final BusinessType businessType;

    public ScoreTarget(Long id,
                       String sidoName,
                       String sigunguName,
                       Integer nx,
                       Integer ny,
                       String commercialAreaCode,
                       BusinessType businessType) {
        this.id = id;
        this.sidoName = sidoName;
        this.sigunguName = sigunguName;
        this.nx = nx;
        this.ny = ny;
        this.commercialAreaCode = commercialAreaCode;
        this.businessType = businessType;
    }

    public Long getId() {
        return id;
    }

    public String getSidoName() {
        return sidoName;
    }

    public String getSigunguName() {
        return sigunguName;
    }

    public Integer getNx() {
        return nx;
    }

    public Integer getNy() {
        return ny;
    }

    public String getCommercialAreaCode() {
        return commercialAreaCode;
    }

    public BusinessType getBusinessType() {
        return businessType;
    }


    public static ScoreTarget from(Store store) {
        return new ScoreTarget(
                store.getId(),
                store.getSidoName(),
                store.getSigunguName(),
                store.getNx(),
                store.getNy(),
                store.getCommercialAreaCode(),
                store.getBusinessType()
        );
    }

    public static ScoreTarget from(GuestRegion region) {
        // 게스트 지역은 상권과 업종이 없어 상권별 요일 가중치를 쓸 수 없다.
        return new ScoreTarget(
                region.getId(),
                region.getSidoName(),
                region.getSigunguName(),
                region.getNx(),
                region.getNy(),
                null,
                null
        );
    }






}
