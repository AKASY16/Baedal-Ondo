package com.baedalondo.api.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 시도 제한과 계정 잠금 동작을 확인한다.

 창이 지나면 다시 세고 잠금이 풀리는지는 시간이 흘러야 알 수 있는데, 실제로 기다리면
 테스트가 10분짜리가 된다. ExternalCallGuard와 같은 방식으로 시계를 넣어 시간만 옮긴다.
 **/
class LoginAttemptGuardTest {

    private static final String ADDRESS = "203.0.113.10";
    private static final String OTHER_ADDRESS = "203.0.113.11";
    private static final String LOGIN_ID = "owner01";

    private Instant now = Instant.parse("2026-09-03T10:00:00Z");

    private final LoginAttemptGuard guard = new LoginAttemptGuard(
            3,
            Duration.ofMinutes(1),
            3,
            Duration.ofMinutes(10),
            () -> now
    );

    @Test
    @DisplayName("허용치까지는 통과시키고 그 다음부터 막는다")
    void blocksOnlyAfterTheAllowedAttempts() {
        assertFalse(guard.tooManyAttempts(ADDRESS));
        assertFalse(guard.tooManyAttempts(ADDRESS));
        assertFalse(guard.tooManyAttempts(ADDRESS));

        assertTrue(guard.tooManyAttempts(ADDRESS));
    }

    @Test
    @DisplayName("주소가 다르면 서로의 횟수에 영향을 주지 않는다")
    void countsEachAddressSeparately() {
        guard.tooManyAttempts(ADDRESS);
        guard.tooManyAttempts(ADDRESS);
        guard.tooManyAttempts(ADDRESS);

        assertFalse(guard.tooManyAttempts(OTHER_ADDRESS));
    }

    @Test
    @DisplayName("창이 지나면 다시 처음부터 센다")
    void startsANewWindowAfterTheWindowPasses() {
        guard.tooManyAttempts(ADDRESS);
        guard.tooManyAttempts(ADDRESS);
        guard.tooManyAttempts(ADDRESS);
        assertTrue(guard.tooManyAttempts(ADDRESS));

        now = now.plus(Duration.ofMinutes(1));

        assertFalse(guard.tooManyAttempts(ADDRESS));
    }

    @Test
    @DisplayName("실패가 쌓이면 계정을 잠근다")
    void locksAccountAfterRepeatedFailures() {
        assertFalse(guard.isLocked(LOGIN_ID));

        guard.recordFailure(LOGIN_ID);
        guard.recordFailure(LOGIN_ID);
        assertFalse(guard.isLocked(LOGIN_ID));

        guard.recordFailure(LOGIN_ID);
        assertTrue(guard.isLocked(LOGIN_ID));
    }

    @Test
    @DisplayName("잠금 시간이 지나면 스스로 풀린다")
    void releasesTheAccountAfterTheLockPasses() {
        guard.recordFailure(LOGIN_ID);
        guard.recordFailure(LOGIN_ID);
        guard.recordFailure(LOGIN_ID);
        assertTrue(guard.isLocked(LOGIN_ID));

        now = now.plus(Duration.ofMinutes(10));

        assertFalse(guard.isLocked(LOGIN_ID));
    }

    @Test
    @DisplayName("실패가 오래 전이면 이어서 세지 않는다")
    void doesNotCarryOldFailuresIntoALock() {
        guard.recordFailure(LOGIN_ID);
        guard.recordFailure(LOGIN_ID);

        now = now.plus(Duration.ofMinutes(10));

        // 어제 두 번 틀린 사람이 오늘 한 번 더 틀렸다고 잠기면 안 된다.
        guard.recordFailure(LOGIN_ID);

        assertFalse(guard.isLocked(LOGIN_ID));
    }

    @Test
    @DisplayName("로그인에 성공하면 쌓인 실패를 지운다")
    void clearsFailuresOnSuccess() {
        guard.recordFailure(LOGIN_ID);
        guard.recordFailure(LOGIN_ID);

        guard.recordSuccess(LOGIN_ID);
        guard.recordFailure(LOGIN_ID);

        assertFalse(guard.isLocked(LOGIN_ID));
    }

    @Test
    @DisplayName("대소문자만 바꿔서는 실패 횟수를 초기화할 수 없다")
    void treatsTheSameLoginIdRegardlessOfCase() {
        guard.recordFailure("owner01");
        guard.recordFailure("Owner01");
        guard.recordFailure("OWNER01");

        assertTrue(guard.isLocked("owner01"));
    }

    @Test
    @DisplayName("아이디를 비워 보내면 계정 기준으로는 세지 않는다")
    void ignoresBlankLoginId() {
        guard.recordFailure("");
        guard.recordFailure(null);
        guard.recordFailure("   ");

        assertFalse(guard.isLocked(""));
        assertFalse(guard.isLocked(null));
    }
}
