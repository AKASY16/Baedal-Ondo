package com.baedalondo.api.weather.service;

import com.baedalondo.api.common.ExternalCallGuard;
import com.baedalondo.api.guest.service.GuestRegionService;
import com.baedalondo.api.score.calculator.KmaTimeCalculator;
import com.baedalondo.api.score.dto.ScoreTarget;
import com.baedalondo.api.store.repository.StoreRepository;
import com.baedalondo.api.weather.client.KmaForecastWeatherClient;
import com.baedalondo.api.weather.domain.ForecastWeatherObservation;
import com.baedalondo.api.weather.exception.KmaWeatherApiException;
import com.baedalondo.api.weather.repository.ForecastWeatherRecordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 예보 조회의 재시도와 쿨다운을 확인한다.

 격자마다 키가 다르므로 한 격자의 실패가 다른 격자를 막지 않아야 한다.
 대시보드는 게스트 지역 16개 격자를 함께 쓰기 때문에 여기서 키가 뭉치면
 한 곳의 장애가 전체 화면에서 날씨를 지운다.
 **/
class ForecastWeatherServiceTest {

    private static final LocalDateTime BASE_DATE_TIME = LocalDateTime.of(2026, 8, 20, 14, 30);

    private final KmaForecastWeatherClient kmaForecastWeatherClient =
            mock(KmaForecastWeatherClient.class);
    private final KmaTimeCalculator kmaTimeCalculator =
            mock(KmaTimeCalculator.class);
    private final ForecastWeatherRecordRepository forecastWeatherRecordRepository =
            mock(ForecastWeatherRecordRepository.class);
    private final StoreRepository storeRepository =
            mock(StoreRepository.class);
    private final GuestRegionService guestRegionService =
            mock(GuestRegionService.class);

    private Instant now = Instant.parse("2026-08-20T05:35:00Z");

    private final ExternalCallGuard externalCallGuard =
            new ExternalCallGuard(Duration.ofSeconds(60), () -> now);

    private final ForecastWeatherService forecastWeatherService = new ForecastWeatherService(
            kmaForecastWeatherClient,
            kmaTimeCalculator,
            forecastWeatherRecordRepository,
            storeRepository,
            guestRegionService,
            externalCallGuard
    );

    @Test
    @DisplayName("타임아웃이면 한 번 더 부르고, 두 번째가 성공하면 그 값을 쓴다")
    void retriesOnceOnTimeout() {
        List<ForecastWeatherObservation> forecast = List.of(observation());

        when(kmaTimeCalculator.getSafeForecastBaseDateTime()).thenReturn(BASE_DATE_TIME);
        when(kmaForecastWeatherClient.getForecastWeather(anyInt(), anyInt(), anyString(), anyString()))
                .thenThrow(timeout())
                .thenReturn(forecast);

        List<ForecastWeatherObservation> result =
                forecastWeatherService.getForecastWeather(scoreTarget(60, 127));

        assertEquals(1, result.size());
        verify(kmaForecastWeatherClient, times(2))
                .getForecastWeather(60, 127, "20260820", "1430");
    }

    @Test
    @DisplayName("필수 항목 누락은 재시도하지 않는다")
    void doesNotRetryOnMissingCategory() {
        // 응답을 받아서 해석한 결과다. 다시 불러도 같은 응답이 온다.
        when(kmaTimeCalculator.getSafeForecastBaseDateTime()).thenReturn(BASE_DATE_TIME);
        when(kmaForecastWeatherClient.getForecastWeather(anyInt(), anyInt(), anyString(), anyString()))
                .thenThrow(new KmaWeatherApiException("예보에 필수 날씨 항목이 누락되었습니다."));

        assertThrows(KmaWeatherApiException.class,
                () -> forecastWeatherService.getForecastWeather(scoreTarget(60, 127)));

        verify(kmaForecastWeatherClient, times(1))
                .getForecastWeather(60, 127, "20260820", "1430");
    }

    @Test
    @DisplayName("두 번 다 실패하면 다음 요청은 API를 부르지 않는다")
    void skipsApiDuringCooldown() {
        when(kmaTimeCalculator.getSafeForecastBaseDateTime()).thenReturn(BASE_DATE_TIME);
        when(kmaForecastWeatherClient.getForecastWeather(anyInt(), anyInt(), anyString(), anyString()))
                .thenThrow(timeout());

        assertThrows(KmaWeatherApiException.class,
                () -> forecastWeatherService.getForecastWeather(scoreTarget(60, 127)));
        verify(kmaForecastWeatherClient, times(2))
                .getForecastWeather(60, 127, "20260820", "1430");

        assertThrows(KmaWeatherApiException.class,
                () -> forecastWeatherService.getForecastWeather(scoreTarget(60, 127)));
        verify(kmaForecastWeatherClient, times(2))
                .getForecastWeather(60, 127, "20260820", "1430");
    }

    @Test
    @DisplayName("한 격자의 쿨다운이 다른 격자를 막지 않는다")
    void isolatesCooldownByGrid() {
        List<ForecastWeatherObservation> forecast = List.of(observation());

        when(kmaTimeCalculator.getSafeForecastBaseDateTime()).thenReturn(BASE_DATE_TIME);
        when(kmaForecastWeatherClient.getForecastWeather(60, 127, "20260820", "1430"))
                .thenThrow(timeout());
        when(kmaForecastWeatherClient.getForecastWeather(61, 126, "20260820", "1430"))
                .thenReturn(forecast);

        assertThrows(KmaWeatherApiException.class,
                () -> forecastWeatherService.getForecastWeather(scoreTarget(60, 127)));

        assertEquals(1, forecastWeatherService.getForecastWeather(scoreTarget(61, 126)).size());
    }

    @Test
    @DisplayName("60초가 지나면 다시 호출한다")
    void callsApiAgainAfterCooldownExpires() {
        AtomicInteger calls = new AtomicInteger();

        when(kmaTimeCalculator.getSafeForecastBaseDateTime()).thenReturn(BASE_DATE_TIME);
        when(kmaForecastWeatherClient.getForecastWeather(anyInt(), anyInt(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    if (calls.incrementAndGet() <= 2) {
                        throw timeout();
                    }
                    return List.of(observation());
                });

        assertThrows(KmaWeatherApiException.class,
                () -> forecastWeatherService.getForecastWeather(scoreTarget(60, 127)));
        assertEquals(2, calls.get());

        now = now.plusSeconds(60);

        assertEquals(1, forecastWeatherService.getForecastWeather(scoreTarget(60, 127)).size());
        assertEquals(3, calls.get());
    }

    private KmaWeatherApiException timeout() {
        return new KmaWeatherApiException(
                "기상청 API 호출 또는 응답 처리 중 오류가 발생했습니다.",
                new ResourceAccessException("I/O error", new SocketTimeoutException("Read timed out")));
    }

    private ScoreTarget scoreTarget(int nx, int ny) {
        return new ScoreTarget(1L, "서울특별시", "중구", nx, ny, null, null);
    }

    private ForecastWeatherObservation observation() {
        return new ForecastWeatherObservation(
                LocalDateTime.of(2026, 8, 20, 15, 0), 0, 0.0, 28.5, 60, 1.5);
    }
}
