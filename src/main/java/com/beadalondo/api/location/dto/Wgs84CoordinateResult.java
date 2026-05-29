package com.beadalondo.api.location.dto;

public class Wgs84CoordinateResult {
    double WgsX;
    double WgsY;

    public Wgs84CoordinateResult(double WgsX, double WgsY) {
        this.WgsX = WgsX;
        this.WgsY = WgsY;
    }

    public double getWgsX() {
        return WgsX;
    }

    public double getWgsY() {
        return WgsY;
    }

    public void setWgsX(double WgsX) {
        this.WgsX = WgsX;
    }

    public void setWgsY(double WgsY) {
        this.WgsY = WgsY;
    }
}
