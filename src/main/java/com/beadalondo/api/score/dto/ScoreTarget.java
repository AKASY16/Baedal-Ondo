package com.beadalondo.api.score.dto;

import com.beadalondo.api.guest.domain.GuestRegion;
import com.beadalondo.api.store.domain.Store;

public class ScoreTarget {

    private final Long id;
    private final String sidoName;
    private final String sigunguName;
    private final Integer nx;
    private final Integer ny;

    public ScoreTarget(Long id,
                       String sidoName,
                       String sigunguName,
                       Integer nx,
                       Integer ny) {
        this.id = id;
        this.sidoName = sidoName;
        this.sigunguName = sigunguName;
        this.nx = nx;
        this.ny = ny;
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


    public static ScoreTarget from(Store store) {
        return new ScoreTarget(
                store.getId(),
                store.getSidoName(),
                store.getSigunguName(),
                store.getNx(),
                store.getNy()
        );
    }

    public static ScoreTarget from(GuestRegion region) {
        return new ScoreTarget(
                region.getId(),
                region.getSidoName(),
                region.getSigunguName(),
                region.getNx(),
                region.getNy()
        );
    }






}
