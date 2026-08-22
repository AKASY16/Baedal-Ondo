package com.baedalondo.api.airquality.service;

import com.baedalondo.api.airquality.calculator.AirQualityCalculator;
import com.baedalondo.api.airquality.client.AirKoreaAverageAirQualityClient;
import com.baedalondo.api.airquality.client.AirKoreaCurrentAirQualityClient;
import com.baedalondo.api.airquality.domain.AirQualityFetchLog;
import com.baedalondo.api.airquality.domain.CurrentAirQualityObservation;
import com.baedalondo.api.airquality.domain.CurrentAirQualityRecord;
import com.baedalondo.api.airquality.repository.AirQualityFetchLogRepository;
import com.baedalondo.api.airquality.exception.AirKoreaApiException;
import com.baedalondo.api.airquality.repository.CurrentAirQualityRecordRepository;
import com.baedalondo.api.airquality.util.KoreanAddressParser;
import com.baedalondo.api.common.ExternalCallGuard;
import com.baedalondo.api.guest.domain.GuestRegion;
import com.baedalondo.api.guest.service.GuestRegionService;
import com.baedalondo.api.score.dto.ScoreTarget;
import com.baedalondo.api.store.repository.StoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class CurrentAirQualityService {

    private final AirKoreaCurrentAirQualityClient airKoreaCurrentAirQualityClient;
    private final AirKoreaAverageAirQualityClient airKoreaAverageAirQualityClient;
    private final AirQualityCalculator airQualityCalculator;
    private final CurrentAirQualityRecordRepository currentAirQualityRecordRepository;
    private final AirQualityFetchLogRepository airQualityFetchLogRepository;
    private final KoreanAddressParser koreanAddressParser;
    private final StoreRepository storeRepository;
    private final GuestRegionService guestRegionService;
    private final ExternalCallGuard externalCallGuard;

    public CurrentAirQualityService(AirKoreaCurrentAirQualityClient airKoreaCurrentAirQualityClient,
                                    AirKoreaAverageAirQualityClient airKoreaAverageAirQualityClient,
                                    AirQualityCalculator airQualityCalculator,
                                    CurrentAirQualityRecordRepository currentAirQualityRecordRepository,
                                    AirQualityFetchLogRepository airQualityFetchLogRepository,
                                    KoreanAddressParser koreanAddressParser,
                                    StoreRepository storeRepository,
                                    GuestRegionService guestRegionService,
                                    ExternalCallGuard externalCallGuard) {
        this.airKoreaCurrentAirQualityClient = airKoreaCurrentAirQualityClient;
        this.airKoreaAverageAirQualityClient = airKoreaAverageAirQualityClient;
        this.airQualityCalculator = airQualityCalculator;
        this.currentAirQualityRecordRepository = currentAirQualityRecordRepository;
        this.airQualityFetchLogRepository = airQualityFetchLogRepository;
        this.koreanAddressParser = koreanAddressParser;
        this.storeRepository = storeRepository;
        this.guestRegionService = guestRegionService;
        this.externalCallGuard = externalCallGuard;
    }

    public CurrentAirQualityObservation getCurrentAirQuality(ScoreTarget scoreTarget) {
        LocalDateTime expectedBaseTime = airQualityCalculator.getSafeAirQualityBaseTime();
        return getCurrentAirQualityAtBaseTime(scoreTarget, expectedBaseTime);
    }

    public CurrentAirQualityObservation getCurrentAirQuality(ScoreTarget scoreTarget,
                                                             LocalDateTime referenceTime) {
        LocalDateTime expectedBaseTime =
                airQualityCalculator.getSafeAirQualityBaseTime(referenceTime);
        return getCurrentAirQualityAtBaseTime(scoreTarget, expectedBaseTime);
    }

    private CurrentAirQualityObservation getCurrentAirQualityAtBaseTime(
            ScoreTarget scoreTarget,
            LocalDateTime expectedBaseTime) {

        if (scoreTarget == null) {
            throw new IllegalArgumentException("가게 정보가 없습니다.");
        }

        if(scoreTarget.getSidoName()==null || scoreTarget.getSigunguName()==null){
            throw new IllegalArgumentException("가게 주소 정보가 없습니다.");
        }

        String sidoName = koreanAddressParser.extractSidoName(scoreTarget.getSidoName());
        String sigunguName = scoreTarget.getSigunguName();

        try {
            return loadOrFetch(sidoName, sigunguName, expectedBaseTime);
        } catch (DataIntegrityViolationException e) {
            // 같은 측정값을 다른 요청이 먼저 저장했다. 조회부터 다시 하면 그 결과를 읽어 쓴다.
            log.debug("같은 기준시각을 다른 요청이 먼저 저장했습니다. 다시 조회합니다. sidoName={}", sidoName);

            return loadOrFetch(sidoName, sigunguName, expectedBaseTime);
        }
    }

    /**
     조회해서 없으면 받아온다.

     조회와 저장 사이가 벌어져 있어 같은 시도를 동시에 처음 조회하면 둘 다 빈 결과를 보고
     둘 다 저장하러 들어간다. 서버를 새로 띄운 직후, 기준 시각이 넘어가는 순간,
     스케줄러와 사용자 요청이 겹치는 순간에 실제로 일어난다.
     충돌은 호출부에서 잡아 조회부터 다시 한다.
     */
    private CurrentAirQualityObservation loadOrFetch(String sidoName,
                                                     String sigunguName,
                                                     LocalDateTime expectedBaseTime) {

        // 재사용 판단 기준은 측정 시각이 아니라 "이 기준시각 데이터를 이미 받아왔는가"다.
        // 측정 시각으로 판단하면 받아왔지만 그 자치구 측정소가 응답에 없던 경우를
        // 구분하지 못해 매 요청마다 API를 다시 호출하게 된다.
        if (airQualityFetchLogRepository.existsBySidoNameAndBaseTime(sidoName, expectedBaseTime)) {
            return findStoredAirQuality(sidoName, sigunguName, expectedBaseTime);
        }

        String cooldownKey = cooldownKey(sidoName, expectedBaseTime);

        // 이 시도가 방금 두 번 연속 실패했다. 저장된 값으로 돌리지 않고 호출 실패와 같게 끝낸다.
        // 저장된 값 조회에는 시간 조건이 없어서 몇 시간 전 측정값이 현재 값처럼 화면에 나간다.
        // ScoreService가 이 예외를 받아 대기질 점수를 빼고 "확인하지 못했어요"로 표시한다.
        // 쿨다운이 버는 것은 결과가 아니라 시간이다. 4초를 기다린 뒤 같은 답을 하지 않는다.
        if (externalCallGuard.isCoolingDown(cooldownKey)) {
            throw new AirKoreaApiException(
                    "대기질 조회가 연속 실패해 잠시 호출을 멈춘 상태입니다. sidoName=" + sidoName);
        }

        List<CurrentAirQualityObservation> airQualities = externalCallGuard.call(
                cooldownKey,
                () -> airKoreaCurrentAirQualityClient.getCurrentAirQualities(sidoName));

        saveAllAirQualityRecords(airQualities);

        CurrentAirQualityObservation targetAirQuality =
                selectTargetAirQuality(
                        airQualities,
                        sidoName,
                        sigunguName,
                        expectedBaseTime
                );

        // 정상적으로 결과를 만든 경우에만 조회 기록을 남긴다.
        // 위에서 예외가 나면 기록이 없으므로 다음 요청이 다시 시도한다.
        recordFetched(sidoName, expectedBaseTime);

        return targetAirQuality;
    }

    /**
     대시보드가 조회할 수 있는 시도의 대기질을 미리 채운다. 스케줄러가 매시 호출한다.

     현재 배달온도는 서울 지역만 서비스하기 때문에 서울 지역만 조회한다.
     게스트 지역과 등록 매장을 합쳐도 시도는 사실상 서울 하나다.

     게스트 지역을 빼면 매장이 하나도 없을 때 아무것도 채우지 않는다.
     게스트 대시보드는 매장 없이도 열리므로 그 경우 첫 방문자가 외부 호출을 그대로 맞는다.

     시도 하나가 실패해도 나머지는 계속 채운다.
     */
    public int preloadDashboardSidoNames() {
        LocalDateTime expectedBaseTime = airQualityCalculator.getSafeAirQualityBaseTime();
        Set<String> sidoNames = findDashboardSidoNames();
        int loaded = 0;

        for (String sidoName : sidoNames) {
            // 이미 이 기준시각을 받아왔다면 다시 부르지 않는다. 사용자 요청이 먼저 채웠을 수 있다.
            if (airQualityFetchLogRepository.existsBySidoNameAndBaseTime(sidoName, expectedBaseTime)) {
                continue;
            }

            String cooldownKey = cooldownKey(sidoName, expectedBaseTime);

            if (externalCallGuard.isCoolingDown(cooldownKey)) {
                log.info("대기질 사전 적재를 건너뜁니다. 직전 조회가 연속 실패했습니다. sidoName={}", sidoName);
                continue;
            }

            try {
                List<CurrentAirQualityObservation> airQualities = externalCallGuard.call(
                        cooldownKey,
                        () -> airKoreaCurrentAirQualityClient.getCurrentAirQualities(sidoName));

                saveAllAirQualityRecords(airQualities);
                recordFetched(sidoName, expectedBaseTime);
                loaded++;
            } catch (RuntimeException e) {
                log.warn("대기질 사전 적재 실패. sidoName={}", sidoName, e);
            }
        }

        log.info("대기질 사전 적재 완료. 시도 {}개 중 {}개 조회", sidoNames.size(), loaded);
        return loaded;
    }

    /**
     게스트 지역과 등록 매장의 시도를 합친다.

     저장된 시도명은 "서울"과 "서울특별시"가 섞여 있을 수 있어 정규화 후 다시 중복을 제거한다.
     */
    private Set<String> findDashboardSidoNames() {
        Set<String> sidoNames = new LinkedHashSet<>();

        for (GuestRegion region : guestRegionService.getRegions()) {
            addNormalizedSidoName(sidoNames, region.getSidoName());
        }

        for (String rawSidoName : storeRepository.findDistinctSidoNames()) {
            addNormalizedSidoName(sidoNames, rawSidoName);
        }

        return sidoNames;
    }

    private void addNormalizedSidoName(Set<String> sidoNames, String rawSidoName) {
        if (rawSidoName == null) {
            return;
        }

        String sidoName = koreanAddressParser.extractSidoName(rawSidoName);

        if (sidoName != null && !sidoName.isBlank()) {
            sidoNames.add(sidoName);
        }
    }

    /**
     * 이미 조회한 기준시각이면 저장된 측정소 데이터를 쓴다.
     * 그 기준시각에 측정된 것만 쓴다. 이전 시각 값은 없는 것으로 보고 시도 평균으로 넘긴다.
     * 해당 자치구 측정소 데이터가 없으면 시도 평균으로 떨어지는 규칙은 그대로 유지한다.
     *
     * 여기로 오는 경로는 "이번 기준시각을 받아왔다"는 한 가지뿐이다.
     * 못 받은 경우를 이 메서드로 보내면 안 된다. 조회에 시간 조건이 없어
     * 옛 측정값이 현재 값처럼 나가고, 호출 실패와 다른 답을 하게 된다.
     */
    private CurrentAirQualityObservation findStoredAirQuality(String sidoName,
                                                              String sigunguName,
                                                              LocalDateTime expectedBaseTime) {
        Optional<CurrentAirQualityRecord> savedAirQuality =
                currentAirQualityRecordRepository
                        .findTopBySidoNameAndDistrictNameAndMeasuredAtGreaterThanEqualOrderByMeasuredAtDescCreatedAtDesc(
                                sidoName,
                                sigunguName,
                                expectedBaseTime
                        );

        if (savedAirQuality.isPresent()) {
            return savedAirQuality.get().toObservation();
        }

        String cooldownKey = cooldownKey(sidoName, expectedBaseTime);

        // 시도 평균도 같은 게이트웨이를 쓰므로 쿨다운 중이면 부르지 않는다.
        // 여기서 기다리면 외부 호출을 건너뛴 의미가 없어진다.
        if (externalCallGuard.isCoolingDown(cooldownKey)) {
            throw new AirKoreaApiException(
                    "대기질 조회가 연속 실패해 잠시 호출을 멈춘 상태입니다. sidoName=" + sidoName);
        }

        log.warn("저장된 자치구 측정소 데이터가 없어 시도 평균을 사용합니다. sidoName={}, districtName={}",
                sidoName,
                sigunguName);

        return externalCallGuard.call(
                cooldownKey,
                () -> airKoreaAverageAirQualityClient.getHourlyAverage(sidoName, expectedBaseTime));
    }

    /**
     한 번의 호출이 그 시도의 해당 기준시각 데이터를 통째로 가져오므로 둘이 곧 조회 단위다.
     기준시각이 바뀌면 키도 바뀌어 이전 시각의 실패가 다음 시각을 막지 않는다.
     */
    private String cooldownKey(String sidoName, LocalDateTime baseTime) {
        return "airkorea:" + sidoName + ":" + baseTime;
    }

    private void recordFetched(String sidoName, LocalDateTime expectedBaseTime) {
        try {
            airQualityFetchLogRepository.save(AirQualityFetchLog.of(sidoName, expectedBaseTime));
        } catch (DataIntegrityViolationException exception) {
            // 같은 기준시각을 동시에 조회한 다른 요청이 먼저 기록했다. 결과는 동일하므로 넘어간다.
            log.debug("이미 기록된 대기질 조회입니다. sidoName={}, baseTime={}", sidoName, expectedBaseTime);
        }
    }




    // airQualities 리스트에 들어있는 Observation을 하나씩 꺼냄
    // 각 Observation이 이미 DB에 저장된 데이터인지 확인
    // 중복 기준은 sidoName, districtName, stationName, measuredAt 임.
    // 이미 같은 데이터가 있으면 저장하지 않고 다음 Observation으로 넘어감.
    // 없으면 CurrentAirQualityRecord 객체를 새로 만들고 값을 채운 뒤 DB에 저장
    private void saveAllAirQualityRecords(List<CurrentAirQualityObservation> airQualities) {
        for (CurrentAirQualityObservation observation : airQualities) {

            String sidoName = observation.getSidoName();

            String districtName = observation.getStationName();

            String stationName = observation.getStationName();

            LocalDateTime measuredAt = observation.getMeasuredAt();

            boolean alreadyExists =
                    currentAirQualityRecordRepository
                            .existsBySidoNameAndDistrictNameAndStationNameAndMeasuredAt(
                                    sidoName,
                                    districtName,
                                    stationName,
                                    measuredAt
                            );

            if (alreadyExists) {
                continue;
            }
            CurrentAirQualityRecord record =
                    CurrentAirQualityRecord.from(
                            sidoName,
                            districtName,
                            observation
                    );

            currentAirQualityRecordRepository.save(record);
        }
    }

    private CurrentAirQualityObservation selectTargetAirQuality(
            List<CurrentAirQualityObservation> airQualities,
            String sidoName,
            String districtName,
            LocalDateTime baseTimeData
    ) {
        if (airQualities == null || airQualities.isEmpty()) {
            throw new IllegalStateException("사용 가능한 대기질 데이터가 없습니다.");
        }

        return airQualities.stream()
                .filter(observation -> districtName.equals(observation.getStationName()))
                .findFirst()
                .orElseGet(() -> {
                    log.warn(
                            "일치하는 측정소가 없어 시도 평균을 사용합니다. sidoName={}, districtName={}",
                            sidoName,
                            districtName
                    );
                    // 방금 받아온 배치에서 떨어지는 경우라 쿨다운은 아니지만,
                    // 평균도 같은 API라 재시도와 실패 기록은 똑같이 적용해야 한다.
                    return externalCallGuard.call(
                            cooldownKey(sidoName, baseTimeData),
                            () -> airKoreaAverageAirQualityClient.getHourlyAverage(sidoName, baseTimeData));
                });
    }


    private static final Logger log = LoggerFactory.getLogger(CurrentAirQualityService.class);
}
