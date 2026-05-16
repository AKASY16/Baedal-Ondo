package com.beadalondo.api.weather.service;

import com.beadalondo.api.store.domain.Store;
import com.beadalondo.api.weather.CurrentWeatherObservation;
import com.beadalondo.api.weather.client.KmaCurrentWeatherClient;
import org.springframework.stereotype.Service;

@Service
public class CurrentWeatherService {

    private final KmaCurrentWeatherClient kmaCurrentWeatherClient;

    public CurrentWeatherService(KmaCurrentWeatherClient kmaCurrentWeatherClient) {
        this.kmaCurrentWeatherClient = kmaCurrentWeatherClient;
    }

    public CurrentWeatherObservation getCurrentWeather(Store store) {
        if (store == null) {
            throw new IllegalArgumentException("가게 정보가 없습니다.");
        }

        if (store.getNx() == null || store.getNy() == null) {
            throw new IllegalStateException("가게의 기상청 격자 좌표가 없습니다.");
        }

        return kmaCurrentWeatherClient.getCurrentWeather(
                store.getNx(),
                store.getNy()
        );
    }
}