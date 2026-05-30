package com.beadalondo.api.location;

import com.beadalondo.api.location.calculator.Epsg5179ToWgs84Converter;
import com.beadalondo.api.location.calculator.Wgs84ToWeatherGridConverter;
import com.beadalondo.api.location.client.JusoCoordinateClient;
import com.beadalondo.api.location.dto.*;
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

        EntCoordinateResult entCoordinate =
                jusoCoordinateClient.getCoordinate(jusoAddress);

        Wgs84CoordinateResult wgsCoordinate =
                epsg5179ToWgs84Converter.epsg5179ToWgs84Converter(entCoordinate.getEntX(), entCoordinate.getEntY());

        WeatherGridResult weatherGridCoordinate =
                wgs84ToWeatherGridConverter.wgs84ToWeatherGridConverter(wgsCoordinate.getWgsX(), wgsCoordinate.getWgsY());


        return weatherGridCoordinate;

    }

}
