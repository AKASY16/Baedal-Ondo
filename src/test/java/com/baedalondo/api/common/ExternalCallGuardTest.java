package com.baedalondo.api.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 재시도 대상 판정과 쿨다운 동작을 확인한다.

 클라이언트가 예외를 자기 타입으로 감싸므로 판정은 원인 사슬을 따라가야 한다.
 맨 위 예외만 보면 타임아웃과 resultCode 오류가 똑같이 AirKoreaApiException으로 보인다.
 **/
class ExternalCallGuardTest {

    private static final String KEY = "airkorea:서울:2026-08-20T15:00";
    private static final String OTHER_KEY = "airkorea:부산:2026-08-20T15:00";

    private Instant now = Instant.parse("2026-08-20T06:00:00Z");

    private final ExternalCallGuard guard =
            new ExternalCallGuard(Duration.ofSeconds(60), () -> now);

    @Test
    @DisplayName("성공하면 한 번만 부른다")
    void callsOnceWhenSuccessful() {
        AtomicInteger calls = new AtomicInteger();

        String result = guard.call(KEY, () -> {
            calls.incrementAndGet();
            return "성공";
        });

        assertEquals("성공", result);
        assertEquals(1, calls.get());
    }

    @Test
    @DisplayName("읽기 타임아웃이면 한 번 더 부르고, 두 번째가 성공하면 그 값을 쓴다")
    void retriesOnceOnReadTimeout() {
        // 에어코리아는 정상이면 150ms 안팎이고 아니면 게이트웨이 타임아웃까지 붙잡는다.
        // 곧바로 다시 부르면 상당수가 성공해서 재시도가 의미를 갖는다.
        AtomicInteger calls = new AtomicInteger();

        String result = guard.call(KEY, () -> {
            if (calls.incrementAndGet() == 1) {
                throw wrapped(new ResourceAccessException("I/O error", new SocketTimeoutException("Read timed out")));
            }
            return "성공";
        });

        assertEquals("성공", result);
        assertEquals(2, calls.get());
        assertFalse(guard.isCoolingDown(KEY), "재시도가 성공했는데 쿨다운에 들어갔다");
    }

    @Test
    @DisplayName("연결 실패도 재시도한다")
    void retriesOnConnectFailure() {
        assertEquals(2, countAttempts(() -> wrapped(new ResourceAccessException("연결 실패", new ConnectException()))));
    }

    @Test
    @DisplayName("502, 503, 504는 재시도한다")
    void retriesOnRetryableServerErrors() {
        for (HttpStatus status : List.of(HttpStatus.BAD_GATEWAY,
                HttpStatus.SERVICE_UNAVAILABLE,
                HttpStatus.GATEWAY_TIMEOUT)) {
            ExternalCallGuard freshGuard = new ExternalCallGuard(Duration.ofSeconds(60), () -> now);
            AtomicInteger calls = new AtomicInteger();

            assertThrows(RuntimeException.class, () -> freshGuard.call(KEY, () -> {
                calls.incrementAndGet();
                throw wrapped(HttpServerErrorException.create(status, status.name(), null, null, null));
            }));

            assertEquals(2, calls.get(), status + "를 재시도하지 않았다");
        }
    }

    @Test
    @DisplayName("resultCode 오류는 재시도하지 않는다")
    void doesNotRetryResultCodeError() {
        // 상대가 정상적으로 응답한 실패다. 다시 불러도 같은 답이 오고 일일 호출 한도만 태운다.
        assertEquals(1, countAttempts(() -> new RuntimeException("에어코리아 API 에러, resultCode=99")));
    }

    @Test
    @DisplayName("인증 오류와 잘못된 요청은 재시도하지 않는다")
    void doesNotRetryClientErrors() {
        assertEquals(1, countAttempts(() ->
                wrapped(HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Unauthorized", null, null, null))));
        assertEquals(1, countAttempts(() ->
                wrapped(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request", null, null, null))));
    }

    @Test
    @DisplayName("500은 재시도하지 않는다")
    void doesNotRetryInternalServerError() {
        // 상대 로직이 실패한 것이라 다시 불러도 같은 결과가 온다.
        // 502·503·504는 게이트웨이가 상류에 닿지 못한 상태라 잠시 뒤 성공할 수 있다.
        assertEquals(1, countAttempts(() -> wrapped(
                HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "error", null, null, null))));
    }

    @Test
    @DisplayName("파싱 오류는 재시도하지 않는다")
    void doesNotRetryParsingError() {
        assertEquals(1, countAttempts(() -> wrapped(new NumberFormatException("For input string: \"-\""))));
    }

    @Test
    @DisplayName("두 번 모두 실패하면 쿨다운에 들어간다")
    void startsCooldownAfterBothAttemptsFail() {
        failTwice(KEY);

        assertTrue(guard.isCoolingDown(KEY));
    }

    @Test
    @DisplayName("재시도하지 않는 실패는 쿨다운을 걸지 않는다")
    void doesNotStartCooldownWhenNotRetried() {
        // 한 번만 시도했으므로 "두 번 모두 실패"가 아니다.
        assertThrows(RuntimeException.class,
                () -> guard.call(KEY, () -> { throw new RuntimeException("resultCode=99"); }));

        assertFalse(guard.isCoolingDown(KEY));
    }

    @Test
    @DisplayName("쿨다운 중에는 외부 호출을 하지 않는다")
    void skipsExternalCallDuringCooldown() {
        failTwice(KEY);

        AtomicInteger calls = new AtomicInteger();

        assertThrows(ExternalCallCooldownException.class,
                () -> guard.call(KEY, () -> {
                    calls.incrementAndGet();
                    return "호출됨";
                }));

        assertEquals(0, calls.get(), "쿨다운 중인데 외부 API를 불렀다");
    }

    @Test
    @DisplayName("60초가 지나면 다시 호출한다")
    void allowsCallAfterCooldownExpires() {
        failTwice(KEY);

        now = now.plusSeconds(59);
        assertTrue(guard.isCoolingDown(KEY), "59초에 쿨다운이 이미 풀렸다");

        now = now.plusSeconds(1);
        assertFalse(guard.isCoolingDown(KEY), "60초가 지났는데 쿨다운이 남아 있다");

        assertEquals("성공", guard.call(KEY, () -> "성공"));
    }

    @Test
    @DisplayName("한 대상의 쿨다운이 다른 대상을 막지 않는다")
    void isolatesCooldownByKey() {
        // 키에 기준 시각이 들어 있어 시각이 바뀌면 이전 실패가 다음 조회를 막지 않는다.
        failTwice(KEY);

        assertFalse(guard.isCoolingDown(OTHER_KEY));
        assertEquals("성공", guard.call(OTHER_KEY, () -> "성공"));
    }

    private void failTwice(String key) {
        assertThrows(RuntimeException.class, () -> guard.call(key, () -> {
            throw wrapped(new ResourceAccessException("I/O error", new SocketTimeoutException("Read timed out")));
        }));
    }

    private int countAttempts(Supplier<RuntimeException> failure) {
        ExternalCallGuard freshGuard = new ExternalCallGuard(Duration.ofSeconds(60), () -> now);
        AtomicInteger calls = new AtomicInteger();

        assertThrows(RuntimeException.class, () -> freshGuard.call(KEY, () -> {
            calls.incrementAndGet();
            throw failure.get();
        }));

        return calls.get();
    }

    /**
     클라이언트가 하는 것처럼 자기 예외로 감싼다. 판정이 원인 사슬을 보는지 확인하려면
     맨 위 예외는 네트워크와 무관한 타입이어야 한다.
     */
    private RuntimeException wrapped(Throwable cause) {
        return new RuntimeException("외부 API 호출 또는 응답 처리 중 오류가 발생했습니다.", cause);
    }
}
