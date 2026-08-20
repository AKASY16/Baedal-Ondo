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
import com.baedalondo.api.common.ExternalCallGuard;
import com.baedalondo.api.score.dto.ScoreTarget;
import com.baedalondo.api.store.repository.StoreRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
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
    private final StoreRepository storeRepository =
            mock(StoreRepository.class);

    // 쿨다운이 풀리는 순간을 실제로 기다리지 않고 확인하려고 시계를 직접 쥔다.
    private Instant now = Instant.parse("2026-08-16T13:00:00Z");

    private final ExternalCallGuard externalCallGuard =
            new ExternalCallGuard(Duration.ofSeconds(60), () -> now);

    private final CurrentAirQualityService currentAirQualityService =
            new CurrentAirQualityService(
                    airKoreaClient,
                    averageAirQualityClient,
                    airQualityCalculator,
                    currentAirQualityRecordRepository,
                    airQualityFetchLogRepository,
                    new KoreanAddressParser(),
                    storeRepository,
                    externalCallGuard
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

    @Test
    @DisplayName("타임아웃이면 한 번 더 부르고, 두 번째가 성공하면 그대로 진행한다")
    void retriesOnceWhenAirKoreaTimesOut() {
        ScoreTarget scoreTarget = createScoreTarget("중구");
        CurrentAirQualityObservation observation = createObservation("중구");

        when(airQualityCalculator.getSafeAirQualityBaseTime()).thenReturn(BASE_TIME);
        when(airKoreaClient.getCurrentAirQualities("서울"))
                .thenThrow(timeout())
                .thenReturn(List.of(observation));

        CurrentAirQualityObservation result =
                currentAirQualityService.getCurrentAirQuality(scoreTarget);

        assertSame(observation, result);
        verify(airKoreaClient, times(2)).getCurrentAirQualities("서울");
        verify(airQualityFetchLogRepository).save(any(AirQualityFetchLog.class));
    }

    @Test
    @DisplayName("resultCode 오류는 재시도하지 않는다")
    void doesNotRetryWhenAirKoreaReturnsResultCodeError() {
        // 상대가 정상적으로 응답한 실패다. 다시 불러도 같은 답이 오고 일일 호출 한도만 태운다.
        ScoreTarget scoreTarget = createScoreTarget("중구");

        when(airQualityCalculator.getSafeAirQualityBaseTime()).thenReturn(BASE_TIME);
        when(airKoreaClient.getCurrentAirQualities("서울"))
                .thenThrow(new AirKoreaApiException("에어코리아 API 에러, resultCode=99"));

        assertThrows(AirKoreaApiException.class,
                () -> currentAirQualityService.getCurrentAirQuality(scoreTarget));

        verify(airKoreaClient, times(1)).getCurrentAirQualities("서울");
    }

    @Test
    @DisplayName("두 번 다 실패하면 다음 요청은 API를 부르지 않고 저장된 데이터를 쓴다")
    void usesStoredDataWithoutCallingApiDuringCooldown() {
        // 실패하는 동안 조회 기록이 남지 않아 요청마다 API를 다시 부른다.
        // 재시도까지 얹으면 새로고침 한 번이 호출 두 번이 되므로 쿨다운으로 끊는다.
        ScoreTarget scoreTarget = createScoreTarget("중구");
        CurrentAirQualityRecord storedRecord = mock(CurrentAirQualityRecord.class);
        CurrentAirQualityObservation stored = createObservation("중구");

        when(airQualityCalculator.getSafeAirQualityBaseTime()).thenReturn(BASE_TIME);
        when(airKoreaClient.getCurrentAirQualities("서울")).thenThrow(timeout());

        assertThrows(AirKoreaApiException.class,
                () -> currentAirQualityService.getCurrentAirQuality(scoreTarget));
        verify(airKoreaClient, times(2)).getCurrentAirQualities("서울");

        when(currentAirQualityRecordRepository
                .findTopBySidoNameAndDistrictNameOrderByMeasuredAtDescCreatedAtDesc("서울", "중구"))
                .thenReturn(Optional.of(storedRecord));
        when(storedRecord.toObservation()).thenReturn(stored);

        CurrentAirQualityObservation result =
                currentAirQualityService.getCurrentAirQuality(scoreTarget);

        assertSame(stored, result);
        verify(airKoreaClient, times(2)).getCurrentAirQualities("서울");
    }

    @Test
    @DisplayName("쿨다운 중에는 시도 평균도 부르지 않는다")
    void doesNotCallAverageApiDuringCooldown() {
        // 평균도 같은 게이트웨이를 쓴다. 여기서 기다리면 외부 호출을 건너뛴 의미가 없다.
        ScoreTarget scoreTarget = createScoreTarget("마포구");

        when(airQualityCalculator.getSafeAirQualityBaseTime()).thenReturn(BASE_TIME);
        when(airKoreaClient.getCurrentAirQualities("서울")).thenThrow(timeout());

        assertThrows(AirKoreaApiException.class,
                () -> currentAirQualityService.getCurrentAirQuality(scoreTarget));

        assertThrows(AirKoreaApiException.class,
                () -> currentAirQualityService.getCurrentAirQuality(scoreTarget));

        verify(averageAirQualityClient, never()).getHourlyAverage(anyString(), any());
    }

    @Test
    @DisplayName("실패는 조회 기록에 남기지 않는다")
    void neverRecordsFetchLogOnFailure() {
        // 실패를 성공 기록에 남기면 다음 요청이 이미 받아왔다고 판단해
        // 빈 데이터를 정상으로 취급한다.
        ScoreTarget scoreTarget = createScoreTarget("중구");

        when(airQualityCalculator.getSafeAirQualityBaseTime()).thenReturn(BASE_TIME);
        when(airKoreaClient.getCurrentAirQualities("서울")).thenThrow(timeout());

        assertThrows(AirKoreaApiException.class,
                () -> currentAirQualityService.getCurrentAirQuality(scoreTarget));

        verify(airQualityFetchLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("60초가 지나면 다시 호출한다")
    void callsApiAgainAfterCooldownExpires() {
        ScoreTarget scoreTarget = createScoreTarget("중구");
        CurrentAirQualityObservation observation = createObservation("중구");
        AtomicInteger calls = new AtomicInteger();

        when(airQualityCalculator.getSafeAirQualityBaseTime()).thenReturn(BASE_TIME);
        when(airKoreaClient.getCurrentAirQualities("서울")).thenAnswer(invocation -> {
            if (calls.incrementAndGet() <= 2) {
                throw timeout();
            }
            return List.of(observation);
        });

        assertThrows(AirKoreaApiException.class,
                () -> currentAirQualityService.getCurrentAirQuality(scoreTarget));
        assertEquals(2, calls.get());

        now = now.plusSeconds(60);

        CurrentAirQualityObservation result =
                currentAirQualityService.getCurrentAirQuality(scoreTarget);

        assertSame(observation, result);
        assertEquals(3, calls.get());
    }

    @Test
    @DisplayName("사전 적재는 쿨다운 중인 시도를 건너뛴다")
    void preloadSkipsSidoInCooldown() {
        when(airQualityCalculator.getSafeAirQualityBaseTime()).thenReturn(BASE_TIME);
        when(storeRepository.findDistinctSidoNames()).thenReturn(List.of("서울특별시"));
        when(airKoreaClient.getCurrentAirQualities("서울")).thenThrow(timeout());

        assertEquals(0, currentAirQualityService.preloadStoreSidoNames());
        verify(airKoreaClient, times(2)).getCurrentAirQualities("서울");

        assertEquals(0, currentAirQualityService.preloadStoreSidoNames());
        verify(airKoreaClient, times(2)).getCurrentAirQualities("서울");
    }

    /**
     클라이언트가 하는 것처럼 감싼다. 판정이 원인 사슬을 보므로 안쪽 타입이 중요하다.
     */
    private AirKoreaApiException timeout() {
        return new AirKoreaApiException(
                "에어코리아 API 호출 또는 응답 처리 중 오류가 발생했습니다.",
                new ResourceAccessException("I/O error", new SocketTimeoutException("Read timed out")));
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
