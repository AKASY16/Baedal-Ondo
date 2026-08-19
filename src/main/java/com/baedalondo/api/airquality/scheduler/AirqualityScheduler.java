package com.baedalondo.api.airquality.scheduler;

import com.baedalondo.api.airquality.service.CurrentAirQualityService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AirqualityScheduler {

    private final CurrentAirQualityService currentAirQualityService;

    public AirqualityScheduler(CurrentAirQualityService currentAirQualityService) {
        this.currentAirQualityService = currentAirQualityService;
    }

    /**
     매시 21분에 등록된 매장이 속한 시도의 대기질을 미리 채운다.

     정각 측정값은 15분 내외로 반영되고 AirQualityCalculator는 20분까지 기다렸다가
     기준 시각을 넘긴다. 그 직후인 21분에 돌려야 새 기준 시각을 곧바로 채운다.
     정각 직후에 돌리면 이전 기준 시각을 채우게 되어 20분부터 다시 비어 있다.

     시도 하나에 한 번 호출로 그 시도의 모든 측정소가 채워진다.
     */
    @Scheduled(
            cron = "0 21 * * * *",
            zone = "Asia/Seoul")
    public void preloadStoreAirQuality() {
        currentAirQualityService.preloadStoreSidoNames();
    }

}
