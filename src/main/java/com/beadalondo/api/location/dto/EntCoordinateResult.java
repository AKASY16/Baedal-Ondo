package com.beadalondo.api.location.dto;

public class EntCoordinateResult {
    double entX, entY;

    public EntCoordinateResult(double entX, double entY) {
        this.entX = entX;
        this.entY = entY;
    }

    public double getEntX() {
        return entX;
    }

    public void setEntX(double entX) {
        this.entX = entX;
    }

    public double getEntY() {
        return entY;
    }

    public void setEntY(double entY) {
        this.entY = entY;
    }
}
