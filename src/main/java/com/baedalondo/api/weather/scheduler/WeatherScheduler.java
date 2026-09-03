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
     매시 정각에 대시보드가 조회할 격자의 예보를 미리 채운다.

     초단기예보는 매시 30분 발표분이 45분 무렵부터 조회되는데 기준 시각은 직전 시각을 쓰므로,
     정각에 돌려도 이미 나와 있는 자료를 받는다. 한 번 채우면 그 시간대 내내
     사용자 요청이 캐시를 탄다.

     5분에 돌리던 것을 정각으로 당겼다. 기준 시각은 정각에 넘어가는데 적재가 5분 늦으면
     그 사이의 첫 요청이 격자마다 외부 왕복을 그대로 기다린다. 그 구간에 요청이 겹치면
     같은 격자를 여러 번 부르기도 한다. 대기질은 기준 시각이 20분에 넘어가서 그 뒤에
     돌려야 하지만 예보에는 그런 제약이 없다.

     실황은 적재하지 않는다. 점수에 쓰이지 않고 지난 관측은 ASOS로 소급해 받을 수 있다.
     */
    @Scheduled(
            cron = "0 0 * * * *",
            zone = "Asia/Seoul")
    public void preloadDashboardWeather() {
        forecastWeatherService.preloadDashboardGrids();
    }

}
