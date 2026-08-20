package com.baedalondo.api.common;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 실제 HTTP 스택에서 나온 예외를 재시도 대상으로 판정하는지 확인한다.

 판정은 원인 사슬에 있는 예외 타입을 보는데, 그 타입은 RestClient가 어떤 요청 팩토리를
 쓰는지에 달려 있다. 스프링 버전이 올라가면서 팩토리 구현이 바뀌면 타입도 바뀐다.
 예외를 직접 만들어 넣는 단위 테스트는 그 변화를 잡지 못하므로,
 여기서는 진짜 서버를 띄우고 진짜 타임아웃을 낸다.
 **/
class ExternalCallGuardTimeoutTest {

    private static final Duration READ_TIMEOUT = Duration.ofMillis(300);

    private HttpServer server;
    private RestClient restClient;

    private final AtomicInteger requestCount = new AtomicInteger();
    private final ExternalCallGuard guard =
            new ExternalCallGuard(Duration.ofSeconds(60), Instant::now);

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

        // 응답을 주지 않고 붙잡고 있는다. 클라이언트가 먼저 읽기 타임아웃을 낸다.
        server.createContext("/slow", exchange -> {
            requestCount.incrementAndGet();
            sleep();
            respond(exchange, 200, "{}");
        });

        server.createContext("/gateway-timeout", exchange -> {
            requestCount.incrementAndGet();
            respond(exchange, 504, "Gateway Timeout");
        });

        server.createContext("/bad-request", exchange -> {
            requestCount.incrementAndGet();
            respond(exchange, 400, "Bad Request");
        });

        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(READ_TIMEOUT);

        restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .build();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    @DisplayName("읽기 타임아웃이 나면 한 번 더 부른다")
    void retriesOnRealReadTimeout() {
        long startedAt = System.nanoTime();

        assertThrows(RuntimeException.class, () -> guard.call("slow", () -> get("/slow")));

        assertEquals(2, requestCount.get(), "읽기 타임아웃을 재시도 대상으로 보지 못했다");
        assertTrue(guard.isCoolingDown("slow"), "두 번 모두 실패했는데 쿨다운이 걸리지 않았다");

        // 두 번을 기다린 만큼은 걸리고, 핸들러가 붙잡고 있는 시간까지 끌려가지는 않아야 한다.
        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
        assertTrue(elapsedMillis >= READ_TIMEOUT.toMillis() * 2,
                "타임아웃 두 번을 기다리지 않았다: " + elapsedMillis + "ms");
    }

    @Test
    @DisplayName("504가 오면 한 번 더 부른다")
    void retriesOnRealGatewayTimeout() {
        assertThrows(RuntimeException.class,
                () -> guard.call("gateway", () -> get("/gateway-timeout")));

        assertEquals(2, requestCount.get());
        assertTrue(guard.isCoolingDown("gateway"));
    }

    @Test
    @DisplayName("400이 오면 재시도하지 않는다")
    void doesNotRetryOnRealBadRequest() {
        assertThrows(RuntimeException.class,
                () -> guard.call("badRequest", () -> get("/bad-request")));

        assertEquals(1, requestCount.get(), "잘못된 요청을 재시도했다");
        assertTrue(!guard.isCoolingDown("badRequest"), "한 번만 시도했는데 쿨다운이 걸렸다");
    }

    /**
     클라이언트들이 하는 것처럼 RestClientException을 자기 예외로 감싼다.
     */
    private String get(String path) {
        try {
            return restClient.get().uri(path).retrieve().body(String.class);
        } catch (RestClientException e) {
            throw new IllegalStateException("외부 API 호출 또는 응답 처리 중 오류가 발생했습니다.", e);
        }
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private void sleep() {
        try {
            Thread.sleep(READ_TIMEOUT.toMillis() * 5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
