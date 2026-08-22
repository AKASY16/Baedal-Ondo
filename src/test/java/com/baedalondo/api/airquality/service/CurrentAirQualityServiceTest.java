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
import com.baedalondo.api.guest.domain.GuestRegion;
import com.baedalondo.api.guest.service.GuestRegionService;
import com.baedalondo.api.score.dto.ScoreTarget;
import com.baedalondo.api.store.repository.StoreRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
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

    // 운영은 요청 진입점에서 읽은 시각을 그대로 넘긴다. 테스트도 같은 경로를 탄다.
    // 22:30은 분이 20 이상이라 AirQualityCalculator 규칙상 기준시각이 22:00이 된다.
    private static final LocalDateTime REFERENCE_TIME = LocalDateTime.of(2026, 8, 16, 22, 30);
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
    private final GuestRegionService guestRegionService =
            mock(GuestRegionService.class);

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
                    guestRegionService,
                    externalCallGuard
            );

    @Test
    void getCurrentAirQualityNormalizesSidoNameBeforeCallingAirKorea() {
        ScoreTarget scoreTarget = createScoreTarget("중구");
        CurrentAirQualityObservation observation = createObservation("중구");

        when(airKoreaClient.getCurrentAirQualities("서울"))
                .thenReturn(List.of(observation));

        CurrentAirQualityObservation result =
                currentAirQualityService.getCurrentAirQuality(scoreTarget, REFERENCE_TIME);

        assertEquals(observation, result);
        verify(airKoreaClient).getCurrentAirQualities("서울");
    }

    @Test
    @DisplayName("같은 기준시각 조회 기록이 있으면 API를 호출하지 않고 저장된 데이터를 쓴다")
    void reusesStoredDataWhenFetchLogExistsForCurrentBaseTime() {
        ScoreTarget scoreTarget = createScoreTarget("중구");
        CurrentAirQualityRecord storedRecord = mock(CurrentAirQualityRecord.class);
        CurrentAirQualityObservation stored = createObservation("중구");

        when(airQualityCalculator.getSafeAirQualityBaseTime(REFERENCE_TIME)).thenReturn(BASE_TIME);
        when(airQualityFetchLogRepository.existsBySidoNameAndBaseTime("서울", BASE_TIME))
                .thenReturn(true);
        when(currentAirQualityRecordRepository
                .findTopBySidoNameAndDistrictNameAndMeasuredAtGreaterThanEqualOrderByMeasuredAtDescCreatedAtDesc("서울", "중구", BASE_TIME))
                .thenReturn(Optional.of(storedRecord));
        when(storedRecord.toObservation()).thenReturn(stored);

        CurrentAirQualityObservation result =
                currentAirQualityService.getCurrentAirQuality(scoreTarget, REFERENCE_TIME);

        assertSame(stored, result);
        verify(airKoreaClient, never()).getCurrentAirQualities(anyString());
        verify(airQualityFetchLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("조회 기록이 없으면 API를 호출하고 측정값과 조회 기록을 모두 저장한다")
    void callsApiAndSavesFetchLogWhenNoFetchLogExists() {
        ScoreTarget scoreTarget = createScoreTarget("중구");
        CurrentAirQualityObservation observation = createObservation("중구");

        when(airQualityCalculator.getSafeAirQualityBaseTime(REFERENCE_TIME)).thenReturn(BASE_TIME);
        when(airQualityFetchLogRepository.existsBySidoNameAndBaseTime("서울", BASE_TIME))
                .thenReturn(false);
        when(airKoreaClient.getCurrentAirQualities("서울"))
                .thenReturn(List.of(observation));

        CurrentAirQualityObservation result =
                currentAirQualityService.getCurrentAirQuality(scoreTarget, REFERENCE_TIME);

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

        when(airQualityCalculator.getSafeAirQualityBaseTime(REFERENCE_TIME)).thenReturn(BASE_TIME);
        when(airQualityFetchLogRepository.existsBySidoNameAndBaseTime("서울", PREVIOUS_BASE_TIME))
                .thenReturn(true);
        when(airQualityFetchLogRepository.existsBySidoNameAndBaseTime("서울", BASE_TIME))
                .thenReturn(false);
        when(airKoreaClient.getCurrentAirQualities("서울"))
                .thenReturn(List.of(observation));

        currentAirQualityService.getCurrentAirQuality(scoreTarget, REFERENCE_TIME);

        verify(airKoreaClient).getCurrentAirQualities("서울");
        verify(airQualityFetchLogRepository).save(any(AirQualityFetchLog.class));
    }

    @Test
    @DisplayName("API 호출이 실패하면 조회 기록을 남기지 않는다")
    void doesNotSaveFetchLogWhenApiCallFails() {
        ScoreTarget scoreTarget = createScoreTarget("중구");

        when(airQualityCalculator.getSafeAirQualityBaseTime(REFERENCE_TIME)).thenReturn(BASE_TIME);
        when(airQualityFetchLogRepository.existsBySidoNameAndBaseTime("서울", BASE_TIME))
                .thenReturn(false);
        when(airKoreaClient.getCurrentAirQualities("서울"))
                .thenThrow(new AirKoreaApiException("에어코리아 API 호출 실패"));

        assertThrows(AirKoreaApiException.class,
                () -> currentAirQualityService.getCurrentAirQuality(scoreTarget, REFERENCE_TIME));

        verify(airQualityFetchLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 저장된 측정값은 다시 저장하지 않는다")
    void skipsSavingWhenSameMeasurementAlreadyStored() {
        ScoreTarget scoreTarget = createScoreTarget("중구");
        CurrentAirQualityObservation observation = createObservation("중구");

        when(airQualityCalculator.getSafeAirQualityBaseTime(REFERENCE_TIME)).thenReturn(BASE_TIME);
        when(airQualityFetchLogRepository.existsBySidoNameAndBaseTime("서울", BASE_TIME))
                .thenReturn(false);
        when(airKoreaClient.getCurrentAirQualities("서울"))
                .thenReturn(List.of(observation));
        when(currentAirQualityRecordRepository
                .existsBySidoNameAndDistrictNameAndStationNameAndMeasuredAt(
                        "서울", "중구", "중구", observation.getMeasuredAt()))
                .thenReturn(true);

        currentAirQualityService.getCurrentAirQuality(scoreTarget, REFERENCE_TIME);

        verify(currentAirQualityRecordRepository, never()).save(any(CurrentAirQualityRecord.class));
        verify(airQualityFetchLogRepository).save(any(AirQualityFetchLog.class));
    }

    @Test
    @DisplayName("자치구 측정소가 없으면 시도 평균을 사용한다")
    void getCurrentAirQualityUsesSidoAverageWhenDistrictStationIsMissing() {
        ScoreTarget scoreTarget = createScoreTarget("마포구");
        CurrentAirQualityObservation otherDistrict = createObservation("중구");
        CurrentAirQualityObservation seoulAverage = createAverageObservation();

        when(airQualityCalculator.getSafeAirQualityBaseTime(REFERENCE_TIME)).thenReturn(BASE_TIME);
        when(airQualityFetchLogRepository.existsBySidoNameAndBaseTime("서울", BASE_TIME))
                .thenReturn(false);
        when(airKoreaClient.getCurrentAirQualities("서울"))
                .thenReturn(List.of(otherDistrict));
        when(averageAirQualityClient.getHourlyAverage("서울", BASE_TIME))
                .thenReturn(seoulAverage);

        CurrentAirQualityObservation result =
                currentAirQualityService.getCurrentAirQuality(scoreTarget, REFERENCE_TIME);

        assertSame(seoulAverage, result);
        verify(averageAirQualityClient).getHourlyAverage("서울", BASE_TIME);
    }

    @Test
    @DisplayName("조회 기록은 있지만 저장된 자치구 데이터가 없으면 시도 평균을 사용한다")
    void usesSidoAverageWhenFetchLogExistsButNoStoredDistrictRecord() {
        ScoreTarget scoreTarget = createScoreTarget("마포구");
        CurrentAirQualityObservation seoulAverage = createAverageObservation();

        when(airQualityCalculator.getSafeAirQualityBaseTime(REFERENCE_TIME)).thenReturn(BASE_TIME);
        when(airQualityFetchLogRepository.existsBySidoNameAndBaseTime("서울", BASE_TIME))
                .thenReturn(true);
        when(currentAirQualityRecordRepository
                .findTopBySidoNameAndDistrictNameAndMeasuredAtGreaterThanEqualOrderByMeasuredAtDescCreatedAtDesc("서울", "마포구", BASE_TIME))
                .thenReturn(Optional.empty());
        when(averageAirQualityClient.getHourlyAverage("서울", BASE_TIME))
                .thenReturn(seoulAverage);

        CurrentAirQualityObservation result =
                currentAirQualityService.getCurrentAirQuality(scoreTarget, REFERENCE_TIME);

        assertSame(seoulAverage, result);
        verify(airKoreaClient, never()).getCurrentAirQualities(anyString());
    }

    @Test
    @DisplayName("타임아웃이면 한 번 더 부르고, 두 번째가 성공하면 그대로 진행한다")
    void retriesOnceWhenAirKoreaTimesOut() {
        ScoreTarget scoreTarget = createScoreTarget("중구");
        CurrentAirQualityObservation observation = createObservation("중구");

        when(airQualityCalculator.getSafeAirQualityBaseTime(REFERENCE_TIME)).thenReturn(BASE_TIME);
        when(airKoreaClient.getCurrentAirQualities("서울"))
                .thenThrow(timeout())
                .thenReturn(List.of(observation));

        CurrentAirQualityObservation result =
                currentAirQualityService.getCurrentAirQuality(scoreTarget, REFERENCE_TIME);

        assertSame(observation, result);
        verify(airKoreaClient, times(2)).getCurrentAirQualities("서울");
        verify(airQualityFetchLogRepository).save(any(AirQualityFetchLog.class));
    }

    @Test
    @DisplayName("resultCode 오류는 재시도하지 않는다")
    void doesNotRetryWhenAirKoreaReturnsResultCodeError() {
        // 상대가 정상적으로 응답한 실패다. 다시 불러도 같은 답이 오고 일일 호출 한도만 태운다.
        ScoreTarget scoreTarget = createScoreTarget("중구");

        when(airQualityCalculator.getSafeAirQualityBaseTime(REFERENCE_TIME)).thenReturn(BASE_TIME);
        when(airKoreaClient.getCurrentAirQualities("서울"))
                .thenThrow(new AirKoreaApiException("에어코리아 API 에러, resultCode=99"));

        assertThrows(AirKoreaApiException.class,
                () -> currentAirQualityService.getCurrentAirQuality(scoreTarget, REFERENCE_TIME));

        verify(airKoreaClient, times(1)).getCurrentAirQualities("서울");
    }

    @Test
    @DisplayName("쿨다운 중에는 API를 부르지 않고, 저장된 값으로 돌리지도 않는다")
    void failsFastDuringCooldownWithoutUsingStoredData() {
        // 실패하는 동안 조회 기록이 남지 않아 요청마다 API를 다시 부른다.
        // 재시도까지 얹으면 새로고침 한 번이 호출 두 번이 되므로 쿨다운으로 끊는다.
        //
        // 다만 저장된 값으로 돌리지는 않는다. 그 조회에는 시간 조건이 없어
        // 몇 시간 전 측정값이 현재 값처럼 나간다. 못 받았으면 못 받았다고 하는 편이 맞고,
        // 호출이 실패했을 때와 같은 답이어야 한다.
        ScoreTarget scoreTarget = createScoreTarget("중구");
        CurrentAirQualityRecord storedRecord = mock(CurrentAirQualityRecord.class);

        when(airQualityCalculator.getSafeAirQualityBaseTime(REFERENCE_TIME)).thenReturn(BASE_TIME);
        when(airKoreaClient.getCurrentAirQualities("서울")).thenThrow(timeout());

        assertThrows(AirKoreaApiException.class,
                () -> currentAirQualityService.getCurrentAirQuality(scoreTarget, REFERENCE_TIME));
        verify(airKoreaClient, times(2)).getCurrentAirQualities("서울");

        // 저장된 값이 있어도 쓰지 않는다.
        when(currentAirQualityRecordRepository
                .findTopBySidoNameAndDistrictNameAndMeasuredAtGreaterThanEqualOrderByMeasuredAtDescCreatedAtDesc("서울", "중구", BASE_TIME))
                .thenReturn(Optional.of(storedRecord));

        assertThrows(AirKoreaApiException.class,
                () -> currentAirQualityService.getCurrentAirQuality(scoreTarget, REFERENCE_TIME));

        verify(airKoreaClient, times(2)).getCurrentAirQualities("서울");
        verify(storedRecord, never()).toObservation();
    }

    @Test
    @DisplayName("쿨다운 중에는 시도 평균도 부르지 않는다")
    void doesNotCallAverageApiDuringCooldown() {
        // 평균도 같은 게이트웨이를 쓴다. 여기서 기다리면 외부 호출을 건너뛴 의미가 없다.
        ScoreTarget scoreTarget = createScoreTarget("마포구");

        when(airQualityCalculator.getSafeAirQualityBaseTime(REFERENCE_TIME)).thenReturn(BASE_TIME);
        when(airKoreaClient.getCurrentAirQualities("서울")).thenThrow(timeout());

        assertThrows(AirKoreaApiException.class,
                () -> currentAirQualityService.getCurrentAirQuality(scoreTarget, REFERENCE_TIME));

        assertThrows(AirKoreaApiException.class,
                () -> currentAirQualityService.getCurrentAirQuality(scoreTarget, REFERENCE_TIME));

        verify(averageAirQualityClient, never()).getHourlyAverage(anyString(), any());
    }

    @Test
    @DisplayName("실패는 조회 기록에 남기지 않는다")
    void neverRecordsFetchLogOnFailure() {
        // 실패를 성공 기록에 남기면 다음 요청이 이미 받아왔다고 판단해
        // 빈 데이터를 정상으로 취급한다.
        ScoreTarget scoreTarget = createScoreTarget("중구");

        when(airQualityCalculator.getSafeAirQualityBaseTime(REFERENCE_TIME)).thenReturn(BASE_TIME);
        when(airKoreaClient.getCurrentAirQualities("서울")).thenThrow(timeout());

        assertThrows(AirKoreaApiException.class,
                () -> currentAirQualityService.getCurrentAirQuality(scoreTarget, REFERENCE_TIME));

        verify(airQualityFetchLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("60초가 지나면 다시 호출한다")
    void callsApiAgainAfterCooldownExpires() {
        ScoreTarget scoreTarget = createScoreTarget("중구");
        CurrentAirQualityObservation observation = createObservation("중구");
        AtomicInteger calls = new AtomicInteger();

        when(airQualityCalculator.getSafeAirQualityBaseTime(REFERENCE_TIME)).thenReturn(BASE_TIME);
        when(airKoreaClient.getCurrentAirQualities("서울")).thenAnswer(invocation -> {
            if (calls.incrementAndGet() <= 2) {
                throw timeout();
            }
            return List.of(observation);
        });

        assertThrows(AirKoreaApiException.class,
                () -> currentAirQualityService.getCurrentAirQuality(scoreTarget, REFERENCE_TIME));
        assertEquals(2, calls.get());

        now = now.plusSeconds(60);

        CurrentAirQualityObservation result =
                currentAirQualityService.getCurrentAirQuality(scoreTarget, REFERENCE_TIME);

        assertSame(observation, result);
        assertEquals(3, calls.get());
    }

    @Test
    @DisplayName("사전 적재는 쿨다운 중인 시도를 건너뛴다")
    void preloadSkipsSidoInCooldown() {
        when(airQualityCalculator.getSafeAirQualityBaseTime(REFERENCE_TIME)).thenReturn(BASE_TIME);
        when(storeRepository.findDistinctSidoNames()).thenReturn(List.of("서울특별시"));
        when(airKoreaClient.getCurrentAirQualities("서울")).thenThrow(timeout());

        assertEquals(0, currentAirQualityService.preloadDashboardSidoNames());
        verify(airKoreaClient, times(2)).getCurrentAirQualities("서울");

        assertEquals(0, currentAirQualityService.preloadDashboardSidoNames());
        verify(airKoreaClient, times(2)).getCurrentAirQualities("서울");
    }

    @Test
    @DisplayName("매장이 없어도 게스트 지역의 시도는 사전 적재한다")
    void preloadsGuestRegionSidoWithoutStores() {
        // 게스트 대시보드는 매장 없이도 열린다. 매장만 보면 아무것도 채우지 않아
        // 첫 방문자가 외부 호출을 그대로 맞는다.
        when(airQualityCalculator.getSafeAirQualityBaseTime(REFERENCE_TIME)).thenReturn(BASE_TIME);
        when(storeRepository.findDistinctSidoNames()).thenReturn(List.of());
        when(guestRegionService.getRegions()).thenReturn(List.of(guestRegion("서울특별시")));
        when(airKoreaClient.getCurrentAirQualities("서울"))
                .thenReturn(List.of(createObservation("중구")));

        assertEquals(1, currentAirQualityService.preloadDashboardSidoNames());

        verify(airKoreaClient).getCurrentAirQualities("서울");
        verify(airQualityFetchLogRepository).save(any(AirQualityFetchLog.class));
    }

    @Test
    @DisplayName("게스트 지역과 매장의 시도가 같으면 한 번만 호출한다")
    void callsOncePerSidoAcrossGuestRegionsAndStores() {
        // 게스트 25개 자치구와 서울 매장이 모두 같은 시도라 정규화 후 중복을 제거해야 한다.
        when(airQualityCalculator.getSafeAirQualityBaseTime(REFERENCE_TIME)).thenReturn(BASE_TIME);
        when(storeRepository.findDistinctSidoNames()).thenReturn(List.of("서울"));
        when(guestRegionService.getRegions())
                .thenReturn(List.of(guestRegion("서울특별시"), guestRegion("서울특별시")));
        when(airKoreaClient.getCurrentAirQualities("서울"))
                .thenReturn(List.of(createObservation("중구")));

        assertEquals(1, currentAirQualityService.preloadDashboardSidoNames());

        verify(airKoreaClient, times(1)).getCurrentAirQualities("서울");
    }

    @Test
    @DisplayName("이전 기준시각에 측정된 저장분은 쓰지 않는다")
    void ignoresStoredRecordMeasuredBeforeCurrentBaseTime() {
        // 조회에 시간 조건이 없으면 몇 시간 전 측정값이 현재 값처럼 화면에 나간다.
        // 조회 기록도 기준시각으로 판단하므로 둘의 기준이 같아야 한다.
        ScoreTarget scoreTarget = createScoreTarget("중구");
        CurrentAirQualityObservation seoulAverage = createAverageObservation();

        when(airQualityCalculator.getSafeAirQualityBaseTime(REFERENCE_TIME)).thenReturn(BASE_TIME);
        when(airQualityFetchLogRepository.existsBySidoNameAndBaseTime("서울", BASE_TIME))
                .thenReturn(true);
        when(currentAirQualityRecordRepository
                .findTopBySidoNameAndDistrictNameAndMeasuredAtGreaterThanEqualOrderByMeasuredAtDescCreatedAtDesc(
                        "서울", "중구", BASE_TIME))
                .thenReturn(Optional.empty());
        when(averageAirQualityClient.getHourlyAverage("서울", BASE_TIME))
                .thenReturn(seoulAverage);

        assertSame(seoulAverage, currentAirQualityService.getCurrentAirQuality(scoreTarget, REFERENCE_TIME));
    }

    @Test
    @DisplayName("방금 받아온 배치에서 자치구가 빠졌을 때 평균 호출도 재시도한다")
    void retriesAverageCallOnFreshBatch() {
        // 평균도 같은 API라 재시도와 실패 기록을 똑같이 적용해야 한다.
        ScoreTarget scoreTarget = createScoreTarget("마포구");
        CurrentAirQualityObservation seoulAverage = createAverageObservation();

        when(airQualityCalculator.getSafeAirQualityBaseTime(REFERENCE_TIME)).thenReturn(BASE_TIME);
        when(airKoreaClient.getCurrentAirQualities("서울"))
                .thenReturn(List.of(createObservation("중구")));
        when(averageAirQualityClient.getHourlyAverage("서울", BASE_TIME))
                .thenThrow(timeout())
                .thenReturn(seoulAverage);

        assertSame(seoulAverage, currentAirQualityService.getCurrentAirQuality(scoreTarget, REFERENCE_TIME));

        verify(averageAirQualityClient, times(2)).getHourlyAverage("서울", BASE_TIME);
        verify(airQualityFetchLogRepository).save(any(AirQualityFetchLog.class));
    }

    @Test
    @DisplayName("평균 호출이 두 번 다 실패하면 그 시도도 쿨다운에 들어간다")
    void startsCooldownWhenAverageCallFailsTwice() {
        ScoreTarget scoreTarget = createScoreTarget("마포구");

        when(airQualityCalculator.getSafeAirQualityBaseTime(REFERENCE_TIME)).thenReturn(BASE_TIME);
        when(airKoreaClient.getCurrentAirQualities("서울"))
                .thenReturn(List.of(createObservation("중구")));
        when(averageAirQualityClient.getHourlyAverage("서울", BASE_TIME)).thenThrow(timeout());

        assertThrows(AirKoreaApiException.class,
                () -> currentAirQualityService.getCurrentAirQuality(scoreTarget, REFERENCE_TIME));
        verify(averageAirQualityClient, times(2)).getHourlyAverage("서울", BASE_TIME);
        verify(airQualityFetchLogRepository, never()).save(any());

        // 쿨다운이 걸렸으므로 다음 요청은 메인 API도 부르지 않는다.
        assertThrows(AirKoreaApiException.class,
                () -> currentAirQualityService.getCurrentAirQuality(scoreTarget, REFERENCE_TIME));
        verify(airKoreaClient, times(1)).getCurrentAirQualities("서울");
        verify(averageAirQualityClient, times(2)).getHourlyAverage("서울", BASE_TIME);
    }

    @Test
    @DisplayName("저장할 때 유니크 충돌이 나면 조회부터 다시 한다")
    void rereadsWhenSaveCollides() {
        // 조회와 저장 사이가 벌어져 있어 같은 시도를 동시에 처음 조회하면
        // 둘 다 빈 결과를 보고 둘 다 저장하러 들어간다.
        ScoreTarget scoreTarget = createScoreTarget("중구");
        CurrentAirQualityRecord storedRecord = mock(CurrentAirQualityRecord.class);
        CurrentAirQualityObservation stored = createObservation("중구");

        when(airQualityCalculator.getSafeAirQualityBaseTime(REFERENCE_TIME)).thenReturn(BASE_TIME);
        when(airKoreaClient.getCurrentAirQualities("서울"))
                .thenReturn(List.of(createObservation("중구")));
        when(currentAirQualityRecordRepository.save(any(CurrentAirQualityRecord.class)))
                .thenThrow(new DataIntegrityViolationException("uk_current_air_quality_record"));

        // 첫 조회는 miss, 충돌 뒤 다시 조회하면 먼저 저장한 쪽 기록이 보인다.
        when(airQualityFetchLogRepository.existsBySidoNameAndBaseTime("서울", BASE_TIME))
                .thenReturn(false)
                .thenReturn(true);
        when(currentAirQualityRecordRepository
                .findTopBySidoNameAndDistrictNameAndMeasuredAtGreaterThanEqualOrderByMeasuredAtDescCreatedAtDesc(
                        "서울", "중구", BASE_TIME))
                .thenReturn(Optional.of(storedRecord));
        when(storedRecord.toObservation()).thenReturn(stored);

        assertSame(stored, currentAirQualityService.getCurrentAirQuality(scoreTarget, REFERENCE_TIME));

        verify(airKoreaClient, times(1)).getCurrentAirQualities("서울");
    }

    @Test
    @DisplayName("다시 조회해도 충돌하면 예외를 그대로 올린다")
    void propagatesWhenSecondAttemptAlsoCollides() {
        // 한 번만 다시 시도한다. 계속 충돌하면 동시성이 아니라 다른 문제다.
        ScoreTarget scoreTarget = createScoreTarget("중구");

        when(airQualityCalculator.getSafeAirQualityBaseTime(REFERENCE_TIME)).thenReturn(BASE_TIME);
        when(airKoreaClient.getCurrentAirQualities("서울"))
                .thenReturn(List.of(createObservation("중구")));
        when(currentAirQualityRecordRepository.save(any(CurrentAirQualityRecord.class)))
                .thenThrow(new DataIntegrityViolationException("uk_current_air_quality_record"));

        assertThrows(DataIntegrityViolationException.class,
                () -> currentAirQualityService.getCurrentAirQuality(scoreTarget, REFERENCE_TIME));

        verify(airKoreaClient, times(2)).getCurrentAirQualities("서울");
        verify(airQualityFetchLogRepository, never()).save(any());
    }

    private GuestRegion guestRegion(String sidoName) {
        return new GuestRegion(
                1L, "표시명", null, null, null, null, null,
                sidoName, "중구", null,
                null, null, null,
                null, null, null, null,
                60, 127
        );
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
