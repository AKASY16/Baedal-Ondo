package com.beadalondo.api.airquality.service;

import com.beadalondo.api.airquality.calculator.AirQualityCalculator;
import com.beadalondo.api.airquality.client.AirKoreaCurrentAirQualityClient;
import com.beadalondo.api.airquality.domain.CurrentAirQualityObservation;
import com.beadalondo.api.airquality.repository.CurrentAirQualityRecordRepository;
import com.beadalondo.api.airquality.util.KoreanAddressParser;
import com.beadalondo.api.score.dto.ScoreTarget;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CurrentAirQualityServiceTest {

    private final AirKoreaCurrentAirQualityClient airKoreaClient =
            mock(AirKoreaCurrentAirQualityClient.class);
    private final AirQualityCalculator airQualityCalculator =
            mock(AirQualityCalculator.class);
    private final CurrentAirQualityRecordRepository currentAirQualityRecordRepository =
            mock(CurrentAirQualityRecordRepository.class);
    private final CurrentAirQualityService currentAirQualityService =
            new CurrentAirQualityService(
                    airKoreaClient,
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
                127
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

    private CurrentAirQualityObservation createObservation(String stationName) {
        return new CurrentAirQualityObservation(
                "서울",
                stationName,
                "111121",
                "도시대기",
                LocalDateTime.of(2026, 6, 14, 22, 0),
                25,
                16,
                0.05,
                67,
                2,
                1,
                2,
                1,
                2,
                2
        );
    }
}
