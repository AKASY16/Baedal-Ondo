package com.beadalondo.api.location.calculator;

import com.beadalondo.api.location.dto.Wgs84CoordinateResult;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.ProjCoordinate;
import org.springframework.stereotype.Component;


@Component
public class Epsg5179ToWgs84Converter {

    public Wgs84CoordinateResult epsg5179ToWgs84Converter(double entX,
                                                          double entY) {

        if (entX == 0 || entY == 0) {
            throw new IllegalArgumentException("유효하지 않은 EPSG:5179 좌표입니다.");
        }

        CRSFactory crsFactory = new CRSFactory();

        CoordinateReferenceSystem epsg5179 = crsFactory.createFromName("EPSG:5179");
        CoordinateReferenceSystem wgs84 = crsFactory.createFromName("EPSG:4326");

        CoordinateTransformFactory transformFactory = new CoordinateTransformFactory();
        CoordinateTransform transform = transformFactory.createTransform(epsg5179, wgs84);

        ProjCoordinate source = new ProjCoordinate(entX, entY);
        ProjCoordinate target = new ProjCoordinate();

        transform.transform(source, target);

        double wgsX = target.x; // 경도 longitude
        double wgsY = target.y; // 위도 latitude

        return new Wgs84CoordinateResult(wgsX, wgsY);
    }
}