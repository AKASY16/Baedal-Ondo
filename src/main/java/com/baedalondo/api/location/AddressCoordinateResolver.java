package com.baedalondo.api.location;

import com.baedalondo.api.location.calculator.Epsg5179ToWgs84Converter;
import com.baedalondo.api.location.calculator.Wgs84ToWeatherGridConverter;
import com.baedalondo.api.location.client.JusoCoordinateClient;
import com.baedalondo.api.location.dto.*;
import org.springframework.stereotype.Component;

@Component
public class AddressCoordinateResolver {
    private final JusoCoordinateClient jusoCoordinateClient;
    private final Epsg5179ToWgs84Converter epsg5179ToWgs84Converter;
    private final Wgs84ToWeatherGridConverter wgs84ToWeatherGridConverter;

    public AddressCoordinateResolver(JusoCoordinateClient jusoCoordinateClient,
                                     Epsg5179ToWgs84Converter epsg5179ToWgs84Converter,
                                     Wgs84ToWeatherGridConverter wgs84ToWeatherGridConverter){
        this.jusoCoordinateClient = jusoCoordinateClient;
        this.epsg5179ToWgs84Converter = epsg5179ToWgs84Converter;
        this.wgs84ToWeatherGridConverter = wgs84ToWeatherGridConverter;
    }



    public WeatherGridResult addressCoordinateResolver(JusoAddressRequest jusoAddress){

        return resolveCoordinate(jusoAddress).getWeatherGrid();

    }

    /**
     * 주소 API를 1회만 호출해서 기상청 격자 좌표와 WGS84 좌표를 함께 돌려준다.
     *
     * WGS84 좌표는 호출자가 상권 판별 등에 일시적으로 사용하기 위한 것이며
     * 영속 저장 대상이 아니다.
     */
    public ResolvedCoordinateResult resolveCoordinate(JusoAddressRequest jusoAddress){

        EntCoordinateResult entCoordinate =
                jusoCoordinateClient.getCoordinate(jusoAddress);

        Wgs84CoordinateResult wgsCoordinate =
                epsg5179ToWgs84Converter.epsg5179ToWgs84Converter(entCoordinate.getEntX(), entCoordinate.getEntY());

        WeatherGridResult weatherGridCoordinate =
                wgs84ToWeatherGridConverter.wgs84ToWeatherGridConverter(wgsCoordinate.getWgsX(), wgsCoordinate.getWgsY());


        return new ResolvedCoordinateResult(weatherGridCoordinate, wgsCoordinate);

    }

}
