package com.baedalondo.api.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 외부 API 호출에 재시도 1회와 실패 쿨다운을 적용한다.

 에어코리아를 실측하면 응답이 둘로 갈린다. 정상이면 150ms 안팎이고, 아니면 게이트웨이가
 자기 타임아웃까지 붙잡고 있다가 504를 던진다. 1.3초와 5초 사이에 떨어지는 응답이 없어서
 타임아웃을 늘려도 건질 요청이 없다. 반대로 곧바로 다시 부르면 상당수가 성공한다.
 지연이 아니라 상태가 갈리는 실패라 재시도가 통하고, 그래서 백오프를 두지 않는다.

 다만 실패가 이어지는 동안 재시도까지 얹으면 호출량이 두 배가 된다. 캐시가 빈 채로 뜬 서버는
 요청마다 외부 API를 부르므로 새로고침 횟수가 그대로 호출량이 된다.
 두 번 다 실패한 대상은 쿨다운에 넣어 그동안 외부 호출을 건너뛰고 폴백으로 보낸다.

 쿨다운은 성공 캐시와 분리해 메모리에만 둔다. 실패를 조회 기록에 남기면
 다음 요청이 "이미 받아왔다"고 판단해 빈 데이터를 정상으로 취급한다.
 */
@Component
public class ExternalCallGuard {

    private static final Duration DEFAULT_COOLDOWN = Duration.ofSeconds(60);

    // 게이트웨이가 상류에 닿지 못한 상태라 잠시 뒤 같은 요청이 성공할 수 있다.
    // 500은 넣지 않는다. 상대 로직이 실패한 것이라 다시 불러도 같은 결과가 온다.
    private static final Set<Integer> RETRYABLE_STATUS_CODES = Set.of(502, 503, 504);

    // 예외가 자기 자신이나 서로를 원인으로 물고 있어도 멈추도록 깊이를 제한한다.
    private static final int MAX_CAUSE_DEPTH = 10;

    private final Duration cooldown;
    private final Supplier<Instant> clock;
    private final Map<String, Instant> cooldownUntil = new ConcurrentHashMap<>();

    public ExternalCallGuard() {
        this(DEFAULT_COOLDOWN, Instant::now);
    }

    /**
     쿨다운 길이와 시계를 지정한다. 테스트가 60초를 실제로 기다리지 않고 흐름을 확인한다.

     스프링은 인자 없는 생성자로 이 빈을 만든다. 여기에 @Autowired를 붙이지 말 것.
     */
    public ExternalCallGuard(Duration cooldown, Supplier<Instant> clock) {
        this.cooldown = cooldown;
        this.clock = clock;
    }

    /**
     쿨다운 중인지 확인한다. 호출부는 이 값을 보고 외부 호출 대신 폴백을 선택한다.
     */
    public boolean isCoolingDown(String key) {
        Instant until = cooldownUntil.get(key);

        if (until == null) {
            return false;
        }

        if (clock.get().isBefore(until)) {
            return true;
        }

        // 만료된 항목은 지운다. 두지 않으면 기준 시각이 바뀔 때마다 키가 쌓인다.
        cooldownUntil.remove(key, until);

        return false;
    }

    /**
     외부 호출을 실행한다. 재시도할 수 있는 실패면 한 번만 더 부르고,
     두 번 다 실패하면 그 대상을 쿨다운에 넣는다.
     */
    public <T> T call(String key, Supplier<T> operation) {
        if (isCoolingDown(key)) {
            throw new ExternalCallCooldownException(key);
        }

        try {
            return operation.get();
        } catch (RuntimeException firstFailure) {
            if (!isRetryable(firstFailure)) {
                // 인증 오류, 잘못된 요청, resultCode 오류, 파싱 실패는 다시 불러도 같은 답이 온다.
                // 재시도는 남은 일일 호출 한도만 태운다.
                throw firstFailure;
            }

            log.warn("외부 호출에 실패해 한 번 더 시도합니다. key={}, 원인={}", key, describe(firstFailure));

            try {
                return operation.get();
            } catch (RuntimeException secondFailure) {
                startCooldown(key);
                throw secondFailure;
            }
        }
    }

    private void startCooldown(String key) {
        Instant until = clock.get().plus(cooldown);
        cooldownUntil.put(key, until);

        log.warn("두 번 모두 실패해 {}초 동안 외부 호출을 멈춥니다. key={}", cooldown.toSeconds(), key);
    }

    /**
     클라이언트가 예외를 자기 타입으로 감싸므로 맨 위만 봐서는 알 수 없다.
     원인 사슬을 따라가며 네트워크 수준 실패인지 확인한다.
     */
    private boolean isRetryable(Throwable throwable) {
        Throwable cause = throwable;

        for (int depth = 0; cause != null && depth < MAX_CAUSE_DEPTH; depth++) {
            if (cause instanceof SocketTimeoutException
                    || cause instanceof HttpTimeoutException
                    || cause instanceof ConnectException) {
                return true;
            }

            if (cause instanceof HttpServerErrorException serverError
                    && RETRYABLE_STATUS_CODES.contains(serverError.getStatusCode().value())) {
                return true;
            }

            if (cause == cause.getCause()) {
                break;
            }

            cause = cause.getCause();
        }

        return false;
    }

    /**
     예외 메시지에는 인증키가 붙은 요청 URI가 들어 있을 수 있어 타입 사슬만 남긴다.
     */
    private String describe(Throwable throwable) {
        StringBuilder types = new StringBuilder();
        Throwable cause = throwable;

        for (int depth = 0; cause != null && depth < MAX_CAUSE_DEPTH; depth++) {
            if (depth > 0) {
                types.append(" <- ");
            }

            types.append(cause.getClass().getSimpleName());

            if (cause instanceof HttpServerErrorException serverError) {
                types.append('(').append(serverError.getStatusCode().value()).append(')');
            }

            if (cause == cause.getCause()) {
                break;
            }

            cause = cause.getCause();
        }

        return types.toString();
    }

    private static final Logger log = LoggerFactory.getLogger(ExternalCallGuard.class);
}
