package com.baedalondo.api.airquality.service;

import com.baedalondo.api.airquality.calculator.AirQualityCalculator;
import com.baedalondo.api.airquality.client.AirKoreaAverageAirQualityClient;
import com.baedalondo.api.airquality.client.AirKoreaCurrentAirQualityClient;
import com.baedalondo.api.airquality.domain.CurrentAirQualityObservation;
import com.baedalondo.api.airquality.repository.CurrentAirQualityRecordRepository;
import com.baedalondo.api.airquality.util.KoreanAddressParser;
import com.baedalondo.api.score.dto.ScoreTarget;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CurrentAirQualityServiceTest {

    private final AirKoreaCurrentAirQualityClient airKoreaClient =
            mock(AirKoreaCurrentAirQualityClient.class);
    private final AirKoreaAverageAirQualityClient averageAirQualityClient =
            mock(AirKoreaAverageAirQualityClient.class);
    private final AirQualityCalculator airQualityCalculator =
            mock(AirQualityCalculator.class);
    private final CurrentAirQualityRecordRepository currentAirQualityRecordRepository =
            mock(CurrentAirQualityRecordRepository.class);
    private final CurrentAirQualityService currentAirQualityService =
            new CurrentAirQualityService(
                    airKoreaClient,
                    averageAirQualityClient,
                    airQualityCalculator,
                    currentAirQualityRecordRepository,
                    new KoreanAddressParser()
            );

    @Test
    void getCurrentAirQualityNormalizesSidoNameBeforeCallingAirKorea() {
        ScoreTarget scoreTarget = new ScoreTarget(
                1L,
                "서울특별시",
                "중구",
                60,
                127,
                null,
                null
        );
        CurrentAirQualityObservation observation = createObservation("중구");

        when(currentAirQualityRecordRepository
                .findTopBySidoNameAndDistrictNameOrderByMeasuredAtDescCreatedAtDesc("서울", "중구"))
                .thenReturn(Optional.empty());
        when(airKoreaClient.getCurrentAirQualities("서울"))
                .thenReturn(List.of(observation));

        CurrentAirQualityObservation result =
                currentAirQualityService.getCurrentAirQuality(scoreTarget);

        assertEquals(observation, result);
        verify(airKoreaClient).getCurrentAirQualities("서울");
    }

    @Test
    void getCurrentAirQualityUsesSidoAverageWhenDistrictStationIsMissing() {
        ScoreTarget scoreTarget = new ScoreTarget(
                1L,
                "서울특별시",
                "마포구",
                60,
                127,
                null,
                null
        );
        LocalDateTime baseTime = LocalDateTime.of(2026, 8, 14, 22, 0);
        CurrentAirQualityObservation otherDistrict = createObservation("중구");
        CurrentAirQualityObservation seoulAverage = new CurrentAirQualityObservation(
                "서울",
                "서울 평균",
                null,
                "시도 평균",
                baseTime,
                23,
                12,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(airQualityCalculator.getSafeAirQualityBaseTime())
                .thenReturn(baseTime);
        when(currentAirQualityRecordRepository
                .findTopBySidoNameAndDistrictNameOrderByMeasuredAtDescCreatedAtDesc("서울", "마포구"))
                .thenReturn(Optional.empty());
        when(airKoreaClient.getCurrentAirQualities("서울"))
                .thenReturn(List.of(otherDistrict));
        when(averageAirQualityClient.getHourlyAverage("서울", baseTime))
                .thenReturn(seoulAverage);

        CurrentAirQualityObservation result =
                currentAirQualityService.getCurrentAirQuality(scoreTarget);

        assertSame(seoulAverage, result);
        verify(averageAirQualityClient).getHourlyAverage("서울", baseTime);
    }

    private CurrentAirQualityObservation createObservation(String stationName) {
        return new CurrentAirQualityObservation(
                "서울",
                stationName,
                "111121",
                "도시대기",
                LocalDateTime.of(2026, 6, 14, 22, 0),
                25,
                16,
                67,
                2,
                1,
                2,
                1,
                2
        );
    }
}
