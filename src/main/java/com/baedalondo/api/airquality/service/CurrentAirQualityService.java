package com.baedalondo.api.airquality.service;

import com.baedalondo.api.airquality.calculator.AirQualityCalculator;
import com.baedalondo.api.airquality.client.AirKoreaAverageAirQualityClient;
import com.baedalondo.api.airquality.client.AirKoreaCurrentAirQualityClient;
import com.baedalondo.api.airquality.domain.CurrentAirQualityObservation;
import com.baedalondo.api.airquality.domain.CurrentAirQualityRecord;
import com.baedalondo.api.airquality.repository.CurrentAirQualityRecordRepository;
import com.baedalondo.api.airquality.util.KoreanAddressParser;
import com.baedalondo.api.score.dto.ScoreTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
public class CurrentAirQualityService {

    private final AirKoreaCurrentAirQualityClient airKoreaCurrentAirQualityClient;
    private final AirKoreaAverageAirQualityClient airKoreaAverageAirQualityClient;
    private final AirQualityCalculator airQualityCalculator;
    private final CurrentAirQualityRecordRepository currentAirQualityRecordRepository;
    private final KoreanAddressParser koreanAddressParser;

    public CurrentAirQualityService(AirKoreaCurrentAirQualityClient airKoreaCurrentAirQualityClient,
                                    AirKoreaAverageAirQualityClient airKoreaAverageAirQualityClient,
                                    AirQualityCalculator airQualityCalculator,
                                    CurrentAirQualityRecordRepository currentAirQualityRecordRepository,
                                    KoreanAddressParser koreanAddressParser) {
        this.airKoreaCurrentAirQualityClient = airKoreaCurrentAirQualityClient;
        this.airKoreaAverageAirQualityClient = airKoreaAverageAirQualityClient;
        this.airQualityCalculator = airQualityCalculator;
        this.currentAirQualityRecordRepository = currentAirQualityRecordRepository;
        this.koreanAddressParser = koreanAddressParser;
    }

    public CurrentAirQualityObservation getCurrentAirQuality(ScoreTarget scoreTarget) {

        if (scoreTarget == null) {
            throw new IllegalArgumentException("가게 정보가 없습니다.");
        }

        if(scoreTarget.getSidoName()==null || scoreTarget.getSigunguName()==null){
            throw new IllegalArgumentException("가게 주소 정보가 없습니다.");
        }

        LocalDateTime baseTimeData = airQualityCalculator.getSafeAirQualityBaseTime();
        String sidoName = koreanAddressParser.extractSidoName(scoreTarget.getSidoName());
        String sigunguName = scoreTarget.getSigunguName();

        Optional<CurrentAirQualityRecord> savedAirQuality =
                currentAirQualityRecordRepository.findTopBySidoNameAndDistrictNameOrderByMeasuredAtDescCreatedAtDesc(
                        sidoName,
                        sigunguName
                );

        if(savedAirQuality.isPresent()
                && isReusable(savedAirQuality.get())){
            log.info("저장된 대기질 데이터 재사용: sidoName={}, sigunguName={}, baseTimeData={}",
                    sidoName,
                    sigunguName,
                    baseTimeData);

            return savedAirQuality.get().toObservation();
        }

        List<CurrentAirQualityObservation> airQualities =
                airKoreaCurrentAirQualityClient.getCurrentAirQualities(sidoName);

        saveAllAirQualityRecords(airQualities);

        CurrentAirQualityObservation targetAirQuality =
                selectTargetAirQuality(
                        airQualities,
                        sidoName,
                        sigunguName,
                        baseTimeData
                );

        return targetAirQuality;
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


    private boolean isReusable(CurrentAirQualityRecord record) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        return record.getMeasuredAt()
                .plusMinutes(90)
                .isAfter(now);
    }


    private static final Logger log = LoggerFactory.getLogger(CurrentAirQualityService.class);
}
