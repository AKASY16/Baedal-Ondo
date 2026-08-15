package com.baedalondo.api.airquality.service;

import com.baedalondo.api.airquality.calculator.AirQualityCalculator;
import com.baedalondo.api.airquality.client.AirKoreaAverageAirQualityClient;
import com.baedalondo.api.airquality.client.AirKoreaCurrentAirQualityClient;
import com.baedalondo.api.airquality.domain.AirQualityFetchLog;
import com.baedalondo.api.airquality.domain.CurrentAirQualityObservation;
import com.baedalondo.api.airquality.domain.CurrentAirQualityRecord;
import com.baedalondo.api.airquality.repository.AirQualityFetchLogRepository;
import com.baedalondo.api.airquality.repository.CurrentAirQualityRecordRepository;
import com.baedalondo.api.airquality.util.KoreanAddressParser;
import com.baedalondo.api.score.dto.ScoreTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CurrentAirQualityService {

    private final AirKoreaCurrentAirQualityClient airKoreaCurrentAirQualityClient;
    private final AirKoreaAverageAirQualityClient airKoreaAverageAirQualityClient;
    private final AirQualityCalculator airQualityCalculator;
    private final CurrentAirQualityRecordRepository currentAirQualityRecordRepository;
    private final AirQualityFetchLogRepository airQualityFetchLogRepository;
    private final KoreanAddressParser koreanAddressParser;

    public CurrentAirQualityService(AirKoreaCurrentAirQualityClient airKoreaCurrentAirQualityClient,
                                    AirKoreaAverageAirQualityClient airKoreaAverageAirQualityClient,
                                    AirQualityCalculator airQualityCalculator,
                                    CurrentAirQualityRecordRepository currentAirQualityRecordRepository,
                                    AirQualityFetchLogRepository airQualityFetchLogRepository,
                                    KoreanAddressParser koreanAddressParser) {
        this.airKoreaCurrentAirQualityClient = airKoreaCurrentAirQualityClient;
        this.airKoreaAverageAirQualityClient = airKoreaAverageAirQualityClient;
        this.airQualityCalculator = airQualityCalculator;
        this.currentAirQualityRecordRepository = currentAirQualityRecordRepository;
        this.airQualityFetchLogRepository = airQualityFetchLogRepository;
        this.koreanAddressParser = koreanAddressParser;
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

        List<CurrentAirQualityObservation> airQualities =
                airKoreaCurrentAirQualityClient.getCurrentAirQualities(sidoName);

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

        log.warn("저장된 자치구 측정소 데이터가 없어 시도 평균을 사용합니다. sidoName={}, districtName={}",
                sidoName,
                sigunguName);

        return airKoreaAverageAirQualityClient.getHourlyAverage(sidoName, expectedBaseTime);
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
