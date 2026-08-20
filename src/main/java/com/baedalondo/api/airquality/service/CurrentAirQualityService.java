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
    private final ExternalCallGuard externalCallGuard;

    public CurrentAirQualityService(AirKoreaCurrentAirQualityClient airKoreaCurrentAirQualityClient,
                                    AirKoreaAverageAirQualityClient airKoreaAverageAirQualityClient,
                                    AirQualityCalculator airQualityCalculator,
                                    CurrentAirQualityRecordRepository currentAirQualityRecordRepository,
                                    AirQualityFetchLogRepository airQualityFetchLogRepository,
                                    KoreanAddressParser koreanAddressParser,
                                    StoreRepository storeRepository,
                                    ExternalCallGuard externalCallGuard) {
        this.airKoreaCurrentAirQualityClient = airKoreaCurrentAirQualityClient;
        this.airKoreaAverageAirQualityClient = airKoreaAverageAirQualityClient;
        this.airQualityCalculator = airQualityCalculator;
        this.currentAirQualityRecordRepository = currentAirQualityRecordRepository;
        this.airQualityFetchLogRepository = airQualityFetchLogRepository;
        this.koreanAddressParser = koreanAddressParser;
        this.storeRepository = storeRepository;
        this.externalCallGuard = externalCallGuard;
    }

    public CurrentAirQualityObservation getCurrentAirQuality(ScoreTarget scoreTarget) {

        if (scoreTarget == null) {
            throw new IllegalArgumentException("가게 정보가 없습니다.");
        }

        if(scoreTarget.getSidoName()==null || scoreTarget.getSigunguName()==null){
            throw new IllegalArgumentException("가게 주소 정보가 없습니다.");
        }

        LocalDateTime expectedBaseTime = airQualityCalculator.getSafeAirQualityBaseTime();
        String sidoName = koreanAddressParser.extractSidoName(scoreTarget.getSidoName());
        String sigunguName = scoreTarget.getSigunguName();

        // 재사용 판단 기준은 측정 시각이 아니라 "이 기준시각 데이터를 이미 받아왔는가"다.
        // 측정 시각으로 판단하면 받아왔지만 그 자치구 측정소가 응답에 없던 경우를
        // 구분하지 못해 매 요청마다 API를 다시 호출하게 된다.
        if (airQualityFetchLogRepository.existsBySidoNameAndBaseTime(sidoName, expectedBaseTime)) {
            return findStoredAirQuality(sidoName, sigunguName, expectedBaseTime);
        }

        String cooldownKey = cooldownKey(sidoName, expectedBaseTime);

        // 이 시도가 방금 두 번 연속 실패했다면 기다리지 않고 기존 폴백으로 간다.
        if (externalCallGuard.isCoolingDown(cooldownKey)) {
            return findStoredAirQuality(sidoName, sigunguName, expectedBaseTime);
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
     등록된 매장이 속한 시도의 대기질을 미리 채운다. 스케줄러가 매시 호출한다.

     현재 배달온도는 서울 지역만 서비스하기 때문에 서울 지역만 조회한다.

     시도 하나가 실패해도 나머지는 계속 채운다.
     */
    public int preloadStoreSidoNames() {
        LocalDateTime expectedBaseTime = airQualityCalculator.getSafeAirQualityBaseTime();
        Set<String> sidoNames = findTargetSidoNames();
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
     매장에 저장된 시도명은 "서울"과 "서울특별시"가 섞여 있을 수 있어 정규화 후 다시 중복을 제거한다.
     */
    private Set<String> findTargetSidoNames() {
        Set<String> sidoNames = new LinkedHashSet<>();

        for (String rawSidoName : storeRepository.findDistinctSidoNames()) {
            String sidoName = koreanAddressParser.extractSidoName(rawSidoName);

            if (sidoName != null && !sidoName.isBlank()) {
                sidoNames.add(sidoName);
            }
        }

        return sidoNames;
    }

    /**
     * 이미 조회한 기준시각이면 저장된 측정소 데이터를 쓴다.
     * 해당 자치구 측정소 데이터가 없으면 시도 평균으로 떨어지는 규칙은 그대로 유지한다.
     */
    private CurrentAirQualityObservation findStoredAirQuality(String sidoName,
                                                              String sigunguName,
                                                              LocalDateTime expectedBaseTime) {
        Optional<CurrentAirQualityRecord> savedAirQuality =
                currentAirQualityRecordRepository.findTopBySidoNameAndDistrictNameOrderByMeasuredAtDescCreatedAtDesc(
                        sidoName,
                        sigunguName
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
                    return airKoreaAverageAirQualityClient
                            .getHourlyAverage(sidoName, baseTimeData);
                });
    }


    private static final Logger log = LoggerFactory.getLogger(CurrentAirQualityService.class);
}
