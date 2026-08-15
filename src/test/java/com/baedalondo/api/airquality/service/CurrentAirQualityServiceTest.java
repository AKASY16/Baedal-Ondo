package com.baedalondo.api.airquality.service;

import com.baedalondo.api.airquality.calculator.AirQualityCalculator;
import com.baedalondo.api.airquality.client.AirKoreaAverageAirQualityClient;
import com.baedalondo.api.airquality.client.AirKoreaCurrentAirQualityClient;
import com.baedalondo.api.airquality.domain.AirQualityFetchLog;
import com.baedalondo.api.airquality.domain.CurrentAirQualityObservation;
import com.baedalondo.api.airquality.domain.CurrentAirQualityRecord;
import com.baedalondo.api.airquality.exception.AirKoreaApiException;
import com.baedalondo.api.airquality.repository.AirQualityFetchLogRepository;
import com.baedalondo.api.airquality.repository.CurrentAirQualityRecordRepository;
import com.baedalondo.api.airquality.util.KoreanAddressParser;
import com.baedalondo.api.score.dto.ScoreTarget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CurrentAirQualityServiceTest {

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 8, 16, 22, 0);
    private static final LocalDateTime PREVIOUS_BASE_TIME = BASE_TIME.minusHours(1);

    private final AirKoreaCurrentAirQualityClient airKoreaClient =
            mock(AirKoreaCurrentAirQualityClient.class);
    private final AirKoreaAverageAirQualityClient averageAirQualityClient =
            mock(AirKoreaAverageAirQualityClient.class);
    private final AirQualityCalculator airQualityCalculator =
            mock(AirQualityCalculator.class);
    private final CurrentAirQualityRecordRepository currentAirQualityRecordRepository =
            mock(CurrentAirQualityRecordRepository.class);
    private final AirQualityFetchLogRepository airQualityFetchLogRepository =
            mock(AirQualityFetchLogRepository.class);
    private final CurrentAirQualityService currentAirQualityService =
            new CurrentAirQualityService(
                    airKoreaClient,
                    averageAirQualityClient,
                    airQualityCalculator,
                    currentAirQualityRecordRepository,
                    airQualityFetchLogRepository,
                    new KoreanAddressParser()
            );

    @Test
    void getCurrentAirQualityNormalizesSidoNameBeforeCallingAirKorea() {
        ScoreTarget scoreTarget = createScoreTarget("중구");
        CurrentAirQualityObservation observation = createObservation("중구");

        when(airKoreaClient.getCurrentAirQualities("서울"))
                .thenReturn(List.of(observation));

        CurrentAirQualityObservation result =
                currentAirQualityService.getCurrentAirQuality(scoreTarget);

        assertEquals(observation, result);
        verify(airKoreaClient).getCurrentAirQualities("서울");
    }

    @Test
    @DisplayName("같은 기준시각 조회 기록이 있으면 API를 호출하지 않고 저장된 데이터를 쓴다")
    void reusesStoredDataWhenFetchLogExistsForCurrentBaseTime() {
        ScoreTarget scoreTarget = createScoreTarget("중구");
        CurrentAirQualityRecord storedRecord = mock(CurrentAirQualityRecord.class);
        CurrentAirQualityObservation stored = createObservation("중구");

        when(airQualityCalculator.getSafeAirQualityBaseTime()).thenReturn(BASE_TIME);
        when(airQualityFetchLogRepository.existsBySidoNameAndBaseTime("서울", BASE_TIME))
                .thenReturn(true);
        when(currentAirQualityRecordRepository
                .findTopBySidoNameAndDistrictNameOrderByMeasuredAtDescCreatedAtDesc("서울", "중구"))
                .thenReturn(Optional.of(storedRecord));
        when(storedRecord.toObservation()).thenReturn(stored);

        CurrentAirQualityObservation result =
                currentAirQualityService.getCurrentAirQuality(scoreTarget);

        assertSame(stored, result);
        verify(airKoreaClient, never()).getCurrentAirQualities(anyString());
        verify(airQualityFetchLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("조회 기록이 없으면 API를 호출하고 측정값과 조회 기록을 모두 저장한다")
    void callsApiAndSavesFetchLogWhenNoFetchLogExists() {
        ScoreTarget scoreTarget = createScoreTarget("중구");
        CurrentAirQualityObservation observation = createObservation("중구");

        when(airQualityCalculator.getSafeAirQualityBaseTime()).thenReturn(BASE_TIME);
        when(airQualityFetchLogRepository.existsBySidoNameAndBaseTime("서울", BASE_TIME))
                .thenReturn(false);
        when(airKoreaClient.getCurrentAirQualities("서울"))
                .thenReturn(List.of(observation));

        CurrentAirQualityObservation result =
                currentAirQualityService.getCurrentAirQuality(scoreTarget);

        assertEquals(observation, result);
        verify(airKoreaClient).getCurrentAirQualities("서울");
        verify(currentAirQualityRecordRepository).save(any(CurrentAirQualityRecord.class));
        verify(airQualityFetchLogRepository).save(any(AirQualityFetchLog.class));
    }

    @Test
    @DisplayName("이전 기준시각 기록만 있으면 새 기준시각으로 다시 조회한다")
    void callsApiWhenOnlyPreviousBaseTimeFetchLogExists() {
        ScoreTarget scoreTarget = createScoreTarget("중구");
        CurrentAirQualityObservation observation = createObservation("중구");

        when(airQualityCalculator.getSafeAirQualityBaseTime()).thenReturn(BASE_TIME);
        when(airQualityFetchLogRepository.existsBySidoNameAndBaseTime("서울", PREVIOUS_BASE_TIME))
                .thenReturn(true);
        when(airQualityFetchLogRepository.existsBySidoNameAndBaseTime("서울", BASE_TIME))
                .thenReturn(false);
        when(airKoreaClient.getCurrentAirQualities("서울"))
                .thenReturn(List.of(observation));

        currentAirQualityService.getCurrentAirQuality(scoreTarget);

        verify(airKoreaClient).getCurrentAirQualities("서울");
        verify(airQualityFetchLogRepository).save(any(AirQualityFetchLog.class));
    }

    @Test
    @DisplayName("API 호출이 실패하면 조회 기록을 남기지 않는다")
    void doesNotSaveFetchLogWhenApiCallFails() {
        ScoreTarget scoreTarget = createScoreTarget("중구");

        when(airQualityCalculator.getSafeAirQualityBaseTime()).thenReturn(BASE_TIME);
        when(airQualityFetchLogRepository.existsBySidoNameAndBaseTime("서울", BASE_TIME))
                .thenReturn(false);
        when(airKoreaClient.getCurrentAirQualities("서울"))
                .thenThrow(new AirKoreaApiException("에어코리아 API 호출 실패"));

        assertThrows(AirKoreaApiException.class,
                () -> currentAirQualityService.getCurrentAirQuality(scoreTarget));

        verify(airQualityFetchLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 저장된 측정값은 다시 저장하지 않는다")
    void skipsSavingWhenSameMeasurementAlreadyStored() {
        ScoreTarget scoreTarget = createScoreTarget("중구");
        CurrentAirQualityObservation observation = createObservation("중구");

        when(airQualityCalculator.getSafeAirQualityBaseTime()).thenReturn(BASE_TIME);
        when(airQualityFetchLogRepository.existsBySidoNameAndBaseTime("서울", BASE_TIME))
                .thenReturn(false);
        when(airKoreaClient.getCurrentAirQualities("서울"))
                .thenReturn(List.of(observation));
        when(currentAirQualityRecordRepository
                .existsBySidoNameAndDistrictNameAndStationNameAndMeasuredAt(
                        "서울", "중구", "중구", observation.getMeasuredAt()))
                .thenReturn(true);

        currentAirQualityService.getCurrentAirQuality(scoreTarget);

        verify(currentAirQualityRecordRepository, never()).save(any(CurrentAirQualityRecord.class));
        verify(airQualityFetchLogRepository).save(any(AirQualityFetchLog.class));
    }

    @Test
    @DisplayName("자치구 측정소가 없으면 시도 평균을 사용한다")
    void getCurrentAirQualityUsesSidoAverageWhenDistrictStationIsMissing() {
        ScoreTarget scoreTarget = createScoreTarget("마포구");
        CurrentAirQualityObservation otherDistrict = createObservation("중구");
        CurrentAirQualityObservation seoulAverage = createAverageObservation();

        when(airQualityCalculator.getSafeAirQualityBaseTime()).thenReturn(BASE_TIME);
        when(airQualityFetchLogRepository.existsBySidoNameAndBaseTime("서울", BASE_TIME))
                .thenReturn(false);
        when(airKoreaClient.getCurrentAirQualities("서울"))
                .thenReturn(List.of(otherDistrict));
        when(averageAirQualityClient.getHourlyAverage("서울", BASE_TIME))
                .thenReturn(seoulAverage);

        CurrentAirQualityObservation result =
                currentAirQualityService.getCurrentAirQuality(scoreTarget);

        assertSame(seoulAverage, result);
        verify(averageAirQualityClient).getHourlyAverage("서울", BASE_TIME);
    }

    @Test
    @DisplayName("조회 기록은 있지만 저장된 자치구 데이터가 없으면 시도 평균을 사용한다")
    void usesSidoAverageWhenFetchLogExistsButNoStoredDistrictRecord() {
        ScoreTarget scoreTarget = createScoreTarget("마포구");
        CurrentAirQualityObservation seoulAverage = createAverageObservation();

        when(airQualityCalculator.getSafeAirQualityBaseTime()).thenReturn(BASE_TIME);
        when(airQualityFetchLogRepository.existsBySidoNameAndBaseTime("서울", BASE_TIME))
                .thenReturn(true);
        when(currentAirQualityRecordRepository
                .findTopBySidoNameAndDistrictNameOrderByMeasuredAtDescCreatedAtDesc("서울", "마포구"))
                .thenReturn(Optional.empty());
        when(averageAirQualityClient.getHourlyAverage("서울", BASE_TIME))
                .thenReturn(seoulAverage);

        CurrentAirQualityObservation result =
                currentAirQualityService.getCurrentAirQuality(scoreTarget);

        assertSame(seoulAverage, result);
        verify(airKoreaClient, never()).getCurrentAirQualities(anyString());
    }

    private ScoreTarget createScoreTarget(String sigunguName) {
        return new ScoreTarget(1L, "서울특별시", sigunguName, 60, 127, null, null);
    }

    private CurrentAirQualityObservation createObservation(String stationName) {
        return new CurrentAirQualityObservation(
                "서울",
                stationName,
                "111121",
                "도시대기",
                LocalDateTime.of(2026, 8, 16, 22, 0),
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

    private CurrentAirQualityObservation createAverageObservation() {
        return new CurrentAirQualityObservation(
                "서울",
                "서울 평균",
                null,
                "시도 평균",
                BASE_TIME,
                23,
                12,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
