package com.beadalondo.api.location.dto;

public class WeatherGridResult {
    private int nx;
    private int ny;

    public WeatherGridResult(int nx, int ny) {
        this.nx = nx;
        this.ny = ny;
    }

    public int getNx() {
        return nx;
    }

    public void setNx(int nx) {
        this.nx = nx;
    }

    public int getNy() {
        return ny;
    }

    public void setNy(int ny) {
        this.ny = ny;
    }
}