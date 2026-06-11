package com.beadalondo.api.score.dto;

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
}
