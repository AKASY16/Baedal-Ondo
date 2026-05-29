package com.beadalondo.api.location.calculator;

import com.beadalondo.api.location.dto.WeatherGridResult;
import org.springframework.stereotype.Component;

@Component
public class Wgs84ToWeatherGridConverter {

    public WeatherGridResult wgs84ToWeatherGridConverter(double wgsX,
                                                         double wgsY) {

        // wgsX = longitude, 경도
        // wgsY = latitude, 위도
        double longitude = wgsX;
        double latitude = wgsY;

        double RE = 6371.00877; // 지구 반경(km)
        double GRID = 5.0;      // 격자 간격(km)
        double SLAT1 = 30.0;    // 표준위도 1
        double SLAT2 = 60.0;    // 표준위도 2
        double OLON = 126.0;    // 기준점 경도
        double OLAT = 38.0;     // 기준점 위도
        double XO = 43.0;       // 기준점 X좌표
        double YO = 136.0;      // 기준점 Y좌표

        double DEGRAD = Math.PI / 180.0;

        double re = RE / GRID;
        double slat1 = SLAT1 * DEGRAD;
        double slat2 = SLAT2 * DEGRAD;
        double olon = OLON * DEGRAD;
        double olat = OLAT * DEGRAD;

        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5)
                / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);

        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;

        double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
        ro = re * sf / Math.pow(ro, sn);

        double ra = Math.tan(Math.PI * 0.25 + latitude * DEGRAD * 0.5);
        ra = re * sf / Math.pow(ra, sn);

        double theta = longitude * DEGRAD - olon;

        if (theta > Math.PI) {
            theta -= 2.0 * Math.PI;
        }

        if (theta < -Math.PI) {
            theta += 2.0 * Math.PI;
        }

        theta *= sn;

        int Nx = (int) Math.floor(ra * Math.sin(theta) + XO + 0.5);
        int Ny = (int) Math.floor(ro - ra * Math.cos(theta) + YO + 0.5);

        return new WeatherGridResult(Nx, Ny);
    }
}