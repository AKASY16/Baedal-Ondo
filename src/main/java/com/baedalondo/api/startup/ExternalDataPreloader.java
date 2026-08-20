package com.baedalondo.api.startup;

import com.baedalondo.api.airquality.service.CurrentAirQualityService;
import com.baedalondo.api.common.ServiceTime;
import com.baedalondo.api.holiday.service.HolidayService;
import com.baedalondo.api.weather.service.ForecastWeatherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 서버가 뜨면 외부 데이터를 한 번 채운다.

 스케줄러는 정해진 분에만 돌기 때문에 재시작 직후부터 다음 실행까지 캐시가 비어 있다.
 그 사이에 들어온 요청은 저마다 외부 API를 직접 부르고, 실패하면 조회 기록이 남지 않아
 다음 요청이 또 부른다. 새로고침 횟수가 그대로 호출량이 되어 상대를 더 밀어붙인다.
 시작할 때 한 번 채우면 이 구멍이 닫힌다.

 사전 적재 경로의 재시도는 사용자가 기다리지 않으므로 비용이 사실상 없다.
 같은 재시도를 요청 경로에서 하면 실패마다 타임아웃만큼 화면이 늦어진다.

 실패해도 기동은 계속한다. 스케줄러와 요청 시 조회가 그대로 남아 있어
 사전 적재는 빨라지게 하는 장치지 없으면 안 되는 경로가 아니다.
 */
@Component
public class ExternalDataPreloader {

    private final HolidayService holidayService;
    private final CurrentAirQualityService currentAirQualityService;
    private final ForecastWeatherService forecastWeatherService;
    private final boolean enabled;

    public ExternalDataPreloader(
            HolidayService holidayService,
            CurrentAirQualityService currentAirQualityService,
            ForecastWeatherService forecastWeatherService,
            @Value("${baedalondo.startup-preload.enabled:true}") boolean enabled) {
        this.holidayService = holidayService;
        this.currentAirQualityService = currentAirQualityService;
        this.forecastWeatherService = forecastWeatherService;
        this.enabled = enabled;
    }

    /**
     ApplicationReadyEvent는 톰캣이 이미 요청을 받기 시작한 뒤에 발행된다.
     여기서 시간을 쓰더라도 서비스가 막히지는 않는다.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void preloadOnStartup() {
        if (!enabled) {
            log.info("서버 시작 시 외부 데이터 사전 적재를 건너뜁니다.");
            return;
        }

        // 서버 시간대와 무관하게 한국 기준 날짜를 써야 한다.
        // UTC 서버에서는 자정 직후에 아직 전날로 계산되어 월이 어긋난다.
        LocalDate today = ServiceTime.today();

        preload("공휴일", () -> holidayService.refreshHolidaysForMonthAndNextMonth(
                today.getYear(), today.getMonthValue()));
        preload("대기질", currentAirQualityService::preloadStoreSidoNames);
        preload("예보", forecastWeatherService::preloadDashboardGrids);
    }

    private void preload(String name, Runnable task) {
        try {
            task.run();
        } catch (RuntimeException e) {
            log.warn("서버 시작 시 {} 사전 적재에 실패했습니다. 스케줄러와 요청 시 조회로 채워집니다.", name, e);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(ExternalDataPreloader.class);
}
