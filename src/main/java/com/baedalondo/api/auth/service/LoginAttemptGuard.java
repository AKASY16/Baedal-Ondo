package com.baedalondo.api.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 로그인과 회원가입 시도를 두 축으로 제한한다.

 로그인은 이 서비스에서 제일 비싼 요청이다. 비밀번호 대조에 BCrypt가 도는데, BCrypt는
 무차별 대입을 어렵게 만들려고 일부러 느리게 설계된 알고리즘이다. 막지 않으면 비밀번호가
 뚫리지 않더라도 초당 수십 번의 시도만으로 2GB 인스턴스의 CPU가 로그인 검증에 묶이고
 정상 사용자의 대시보드까지 같이 느려진다. 계정을 지키는 장치이면서 서버를 지키는 장치다.

 축이 둘인 이유는 공격 모양이 둘이기 때문이다. 한 계정에 비밀번호 후보를 계속 넣어보는
 쪽은 계정 기준 잠금이 막고, 유출된 아이디와 비밀번호 쌍을 여러 계정에 한 번씩 넣어보는
 쪽은 계정마다 실패가 하나씩이라 잠금에 걸리지 않으므로 주소 기준 제한이 막는다.

 계정 잠금은 영구가 아니라 시간제다. 영구로 두면 남의 아이디로 일부러 실패를 쌓아
 그 사람을 못 들어오게 만드는 쪽이 새로운 공격이 된다.

 상태는 메모리에만 둔다. 인스턴스가 하나라 충분하고, 재시작하면 풀리지만 재시작은 배포할
 때뿐이라 공격자가 노리고 맞출 수 있는 창이 아니다. 인스턴스를 늘리는 시점에는 앞단으로
 옮기거나 공유 저장소가 필요하다.
 */
@Component
public class LoginAttemptGuard {

    private static final int DEFAULT_MAX_ATTEMPTS_PER_ADDRESS = 10;
    private static final int DEFAULT_MAX_FAILURES_PER_ACCOUNT = 5;

    // 만료된 항목을 걷어내는 주기. 지난 기록을 그대로 두면 다녀간 주소 수만큼 키가 쌓인다.
    private static final Duration SWEEP_INTERVAL = Duration.ofMinutes(1);

    private final int maxAttemptsPerAddress;
    private final Duration attemptWindow;
    private final int maxFailuresPerAccount;
    private final Duration accountLock;
    private final Supplier<Instant> clock;

    private final Map<String, AttemptWindow> attemptsByAddress = new ConcurrentHashMap<>();
    private final Map<String, AccountFailures> failuresByAccount = new ConcurrentHashMap<>();
    private final AtomicReference<Instant> lastSweep;

    /**
     운영 값은 application.yaml에서 읽는다. 제한 값은 실제 공격 양상을 보고 조정하는
     종류라 코드에 박아 두지 않고 환경변수로도 덮을 수 있게 뒀다.

     생성자가 둘이라 어느 쪽으로 빈을 만들지 스프링이 고르지 못한다. 여기라고 알려 준다.
     */
    @Autowired
    public LoginAttemptGuard(
            @Value("${baedalondo.login-protection.max-attempts-per-address:10}") int maxAttemptsPerAddress,
            @Value("${baedalondo.login-protection.attempt-window:1m}") Duration attemptWindow,
            @Value("${baedalondo.login-protection.max-failures-per-account:5}") int maxFailuresPerAccount,
            @Value("${baedalondo.login-protection.account-lock:10m}") Duration accountLock) {
        this(maxAttemptsPerAddress, attemptWindow, maxFailuresPerAccount, accountLock, Instant::now);
    }

    /**
     제한 값과 시계를 지정한다. 테스트가 10분을 실제로 기다리지 않고 잠금이 풀리는 것까지 본다.

     스프링은 위 생성자로 이 빈을 만든다. 여기에 @Autowired를 붙이지 말 것.
     */
    public LoginAttemptGuard(int maxAttemptsPerAddress,
                             Duration attemptWindow,
                             int maxFailuresPerAccount,
                             Duration accountLock,
                             Supplier<Instant> clock) {
        this.maxAttemptsPerAddress = maxAttemptsPerAddress;
        this.attemptWindow = attemptWindow;
        this.maxFailuresPerAccount = maxFailuresPerAccount;
        this.accountLock = accountLock;
        this.clock = clock;
        this.lastSweep = new AtomicReference<>(clock.get());
    }

    /**
     시도를 한 건 세고, 창 안에서 허용치를 넘었는지 답한다.

     고정 창이라 창이 바뀌는 경계에서는 짧은 순간 허용치의 두 배까지 들어올 수 있다.
     초 단위로 정확히 깎는 것이 목적이 아니라 자동화된 반복을 멈추는 것이 목적이라
     여기서는 이 정도면 충분하다고 봤다.
     */
    public boolean tooManyAttempts(String address) {
        Instant now = clock.get();
        sweepIfDue(now);

        AttemptWindow window = attemptsByAddress.compute(address, (key, current) -> {
            if (current == null || hasWindowPassed(current, now)) {
                return new AttemptWindow(now, 1);
            }

            return new AttemptWindow(current.startedAt(), current.count() + 1);
        });

        if (window.count() <= maxAttemptsPerAddress) {
            return false;
        }

        // 여기 찍히는 주소가 Cloudflare 대역이면 Nginx가 실제 접속자 IP를 넘기지 않는 것이다.
        // 그 경우 여러 사용자가 한 칸을 나눠 쓰게 되므로 앞단의 real_ip 설정부터 봐야 한다.
        log.warn("시도가 잦아 인증 요청을 막았습니다. address={}, {}초 동안 {}회",
                address, attemptWindow.toSeconds(), window.count());

        return true;
    }

    /**
     실패가 쌓여 잠긴 계정인지 확인한다. 잠긴 동안에는 비밀번호 대조까지 가지 않는다.
     */
    public boolean isLocked(String loginId) {
        String key = normalize(loginId);

        if (key == null) {
            return false;
        }

        AccountFailures failures = failuresByAccount.get(key);

        if (failures == null || failures.lockedUntil() == null) {
            return false;
        }

        if (clock.get().isBefore(failures.lockedUntil())) {
            return true;
        }

        // 만료된 잠금은 지운다. 남겨 두면 다음 실패가 이미 잠긴 상태에서 시작한다.
        failuresByAccount.remove(key, failures);

        return false;
    }

    /**
     로그인 실패를 기록한다. 계정이 없어서 실패한 경우도 똑같이 센다.
     있는 계정만 잠그면 잠기는지 여부가 곧 그 아이디가 존재한다는 신호가 된다.
     */
    public void recordFailure(String loginId) {
        String key = normalize(loginId);

        if (key == null) {
            return;
        }

        Instant now = clock.get();

        AccountFailures failures = failuresByAccount.compute(key, (ignored, current) -> {
            if (current == null || hasFailureDecayed(current, now)) {
                return new AccountFailures(1, now, null);
            }

            int count = current.count() + 1;
            Instant lockedUntil = count >= maxFailuresPerAccount ? now.plus(accountLock) : null;

            return new AccountFailures(count, now, lockedUntil);
        });

        if (failures.lockedUntil() != null) {
            log.warn("실패가 이어져 계정을 {}분 동안 잠급니다. loginId={}, 실패={}회",
                    accountLock.toMinutes(), key, failures.count());
        }
    }

    /**
     로그인에 성공하면 그 계정의 실패 기록을 지운다. 비밀번호를 몇 번 헷갈렸다가 맞춘
     사람에게 다음 방문까지 실패가 따라다니지 않게 한다.
     */
    public void recordSuccess(String loginId) {
        String key = normalize(loginId);

        if (key != null) {
            failuresByAccount.remove(key);
        }
    }

    private boolean hasWindowPassed(AttemptWindow window, Instant now) {
        return !now.isBefore(window.startedAt().plus(attemptWindow));
    }

    private boolean hasFailureDecayed(AccountFailures failures, Instant now) {
        return !now.isBefore(failures.lastFailureAt().plus(accountLock));
    }

    private void sweepIfDue(Instant now) {
        Instant last = lastSweep.get();

        if (now.isBefore(last.plus(SWEEP_INTERVAL))) {
            return;
        }

        // 요청이 동시에 들어와도 청소는 한 번만 돈다.
        if (!lastSweep.compareAndSet(last, now)) {
            return;
        }

        attemptsByAddress.values().removeIf(window -> hasWindowPassed(window, now));
        failuresByAccount.values().removeIf(failures -> hasFailureDecayed(failures, now)
                && (failures.lockedUntil() == null || !now.isBefore(failures.lockedUntil())));
    }

    /**
     MySQL 기본 collation은 대소문자를 구분하지 않아 Owner01과 owner01이 같은 계정으로
     조회된다. 여기서 구분하면 대소문자만 바꿔가며 실패 횟수를 초기화할 수 있다.
     */
    private String normalize(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            return null;
        }

        return loginId.trim().toLowerCase(Locale.ROOT);
    }

    private record AttemptWindow(Instant startedAt, int count) {
    }

    private record AccountFailures(int count, Instant lastFailureAt, Instant lockedUntil) {
    }

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptGuard.class);
}
