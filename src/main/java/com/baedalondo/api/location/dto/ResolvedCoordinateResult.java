package com.baedalondo.api.location.dto;

/**
 * 주소 1건에 대한 좌표 변환 결과 묶음.
 *
 * WGS84 좌표는 기상청 격자 계산과 상권 판별에만 일시적으로 사용하고
 * DB에 영속 저장하지 않는다. (주소 API 이용약관 제약)
 */
public class ResolvedCoordinateResult {

    private final WeatherGridResult weatherGrid;
    private final Wgs84CoordinateResult wgs84;

    public ResolvedCoordinateResult(WeatherGridResult weatherGrid,
                                    Wgs84CoordinateResult wgs84) {
        this.weatherGrid = weatherGrid;
        this.wgs84 = wgs84;
    }

    public WeatherGridResult getWeatherGrid() {
        return weatherGrid;
    }

    public Wgs84CoordinateResult getWgs84() {
        return wgs84;
    }

    /** 위도 */
    public double getLatitude() {
        return wgs84.getWgsY();
    }

    /** 경도 */
    public double getLongitude() {
        return wgs84.getWgsX();
    }
}
