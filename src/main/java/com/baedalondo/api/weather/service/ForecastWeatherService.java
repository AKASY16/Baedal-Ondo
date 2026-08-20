package com.baedalondo.api.weather.service;

import com.baedalondo.api.common.ExternalCallGuard;
import com.baedalondo.api.score.calculator.KmaTimeCalculator;
import com.baedalondo.api.score.dto.ScoreTarget;
import com.baedalondo.api.weather.client.KmaForecastWeatherClient;
import com.baedalondo.api.weather.domain.ForecastWeatherObservation;
import com.baedalondo.api.weather.domain.ForecastWeatherRecord;
import com.baedalondo.api.weather.domain.WeatherGrid;
import com.baedalondo.api.weather.exception.KmaWeatherApiException;
import com.baedalondo.api.weather.repository.ForecastWeatherRecordRepository;
import com.baedalondo.api.guest.domain.GuestRegion;
import com.baedalondo.api.guest.service.GuestRegionService;
import com.baedalondo.api.store.repository.StoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ForecastWeatherService {

    private static final DateTimeFormatter BASE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter BASE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmm");

    private final KmaForecastWeatherClient kmaForecastWeatherClient;
    private final KmaTimeCalculator kmaTimeCalculator;
    private final ForecastWeatherRecordRepository forecastWeatherRecordRepository;
    private final StoreRepository storeRepository;
    private final GuestRegionService guestRegionService;
    private final ExternalCallGuard externalCallGuard;

    public ForecastWeatherService(
            KmaForecastWeatherClient kmaForecastWeatherClient,
            KmaTimeCalculator kmaTimeCalculator,
            ForecastWeatherRecordRepository forecastWeatherRecordRepository,
            StoreRepository storeRepository,
            GuestRegionService guestRegionService,
            ExternalCallGuard externalCallGuard
    ) {
        this.kmaForecastWeatherClient = kmaForecastWeatherClient;
        this.kmaTimeCalculator = kmaTimeCalculator;
        this.forecastWeatherRecordRepository = forecastWeatherRecordRepository;
        this.storeRepository = storeRepository;
        this.guestRegionService = guestRegionService;
        this.externalCallGuard = externalCallGuard;
    }

    public List<ForecastWeatherObservation> getForecastWeather(ScoreTarget scoreTarget) {
        if (scoreTarget == null) {
            throw new IllegalArgumentException("가게 정보가 없습니다.");
        }

        if (scoreTarget.getNx() == null || scoreTarget.getNy() == null) {
            throw new IllegalStateException("가게의 기상청 격자 좌표가 없습니다.");
        }

        return loadOrFetch(scoreTarget.getNx(), scoreTarget.getNy());
    }

    /**
     대시보드가 조회할 수 있는 격자의 예보를 미리 채운다. 스케줄러가 매시 호출한다.

     게스트 지역과 등록 매장을 합쳐 중복을 제거한다. 게스트 25개 자치구가 격자로는 16개이고,
     서울 매장은 대부분 그 안에 들어오므로 매장이 늘어도 호출 수는 거의 그대로다.

     격자 하나가 실패해도 나머지는 계속 채운다. 사전 적재가 실패해도 사용자 요청이
     기존 경로로 직접 조회하므로 화면이 멈추지는 않는다.
     */
    public int preloadDashboardGrids() {
        Set<WeatherGrid> grids = findDashboardGrids();
        int loaded = 0;

        for (WeatherGrid grid : grids) {
            try {
                loadOrFetch(grid.nx(), grid.ny());
                loaded++;
            } catch (RuntimeException e) {
                log.warn("예보 사전 적재 실패. nx={}, ny={}", grid.nx(), grid.ny(), e);
            }
        }

        log.info("예보 사전 적재 완료. 격자 {}개 중 {}개 성공", grids.size(), loaded);
        return loaded;
    }

    private Set<WeatherGrid> findDashboardGrids() {
        Set<WeatherGrid> grids = new LinkedHashSet<>();

        for (GuestRegion region : guestRegionService.getRegions()) {
            if (region.getNx() != null && region.getNy() != null) {
                grids.add(new WeatherGrid(region.getNx(), region.getNy()));
            }
        }

        grids.addAll(storeRepository.findDistinctWeatherGrids());

        return grids;
    }

    private List<ForecastWeatherObservation> loadOrFetch(int nx, int ny) {
        // 초단기예보는 실황과 발표 주기가 달라 예보 전용 기준 시각을 쓴다.
        LocalDateTime baseDateTime = kmaTimeCalculator.getSafeForecastBaseDateTime();
        String baseDate = baseDateTime.format(BASE_DATE_FORMATTER);
        String baseTime = baseDateTime.format(BASE_TIME_FORMATTER);

        List<ForecastWeatherRecord> savedForecastWeather =
                forecastWeatherRecordRepository.findByNxAndNyAndBaseDateAndBaseTimeOrderByForecastAtAsc(
                        nx, ny, baseDate, baseTime);

        if (!savedForecastWeather.isEmpty()) {
            return savedForecastWeather.stream()
                    .map(ForecastWeatherRecord::toObservation)
                    .toList();
        }

        String cooldownKey = cooldownKey(nx, ny, baseDate, baseTime);

        // 이 격자가 방금 두 번 연속 실패했다면 다시 기다리지 않는다.
        // ScoreService가 이 예외를 받아 날씨 보정만 빼고 화면은 그대로 그린다.
        if (externalCallGuard.isCoolingDown(cooldownKey)) {
            throw new KmaWeatherApiException(
                    "예보 조회가 연속 실패해 잠시 호출을 멈춘 상태입니다. nx=" + nx + ", ny=" + ny);
        }

        List<ForecastWeatherObservation> forecastWeather = externalCallGuard.call(
                cooldownKey,
                () -> kmaForecastWeatherClient.getForecastWeather(
                        nx,
                        ny,
                        baseDate,
                        baseTime
                ));

        // 예보 1건이 레코드 1행이다. 한 번의 호출에서 받은 여러 시각을 모두 저장한다.
        List<ForecastWeatherRecord> records = forecastWeather.stream()
                .map(observation -> ForecastWeatherRecord.from(nx, ny, baseDate, baseTime, observation))
                .toList();

        forecastWeatherRecordRepository.saveAll(records);

        return forecastWeather.stream()
                .sorted(Comparator.comparing(ForecastWeatherObservation::getForecastAt))
                .toList();
    }

    /**
     한 번의 호출이 격자 하나의 해당 발표분을 통째로 가져오므로 넷이 곧 조회 단위다.
     발표 시각이 바뀌면 키도 바뀌어 이전 발표분의 실패가 다음 발표분을 막지 않는다.
     */
    private String cooldownKey(int nx, int ny, String baseDate, String baseTime) {
        return "kma-forecast:" + nx + ":" + ny + ":" + baseDate + ":" + baseTime;
    }

    private static final Logger log = LoggerFactory.getLogger(ForecastWeatherService.class);
}
