package com.baedalondo.api.weather.scheduler;

import com.baedalondo.api.weather.service.ForecastWeatherService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WeatherScheduler {

    private final ForecastWeatherService forecastWeatherService;

    public WeatherScheduler(ForecastWeatherService forecastWeatherService) {
        this.forecastWeatherService = forecastWeatherService;
    }

    /**
     매시 5분에 대시보드가 조회할 격자의 예보를 미리 채운다.

     초단기예보는 매시 30분 발표분이 45분 무렵부터 조회되는데 기준 시각은 직전 시각을 쓰므로,
     정각 직후에 돌려도 이미 나와 있는 자료를 받는다. 한 번 채우면 그 시간대 내내
     사용자 요청이 캐시를 탄다.

     실황은 적재하지 않는다. 점수에 쓰이지 않고 지난 관측은 ASOS로 소급해 받을 수 있다.
     */
    @Scheduled(
            cron = "0 5 * * * *",
            zone = "Asia/Seoul")
    public void preloadDashboardWeather() {
        forecastWeatherService.preloadDashboardGrids();
    }

}
