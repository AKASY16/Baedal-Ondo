package com.baedalondo.api.config;

import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.Duration;

/**
 * 외부 API 호출에 공통 타임아웃을 적용한다.
 *
 * 타임아웃이 없으면 상대 API가 죽지 않고 느려지기만 해도 요청 스레드가 계속 붙잡혀,
 * Tomcat 스레드가 모두 소진되면 서비스 전체가 멈춘다.
 *
 * 기상청, 에어코리아, 도로명주소, 공휴일 클라이언트가 모두 주입받는
 * RestClient.Builder에 이 커스터마이저가 자동으로 적용된다.
 */
@Configuration
public class RestClientConfig {

    // 연결은 즉시 이뤄져야 하므로 짧게 잡는다.
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);

    // 공공 API는 평소 1초 이내지만 피크에 느려지므로 여유를 둔다.
    // 대시보드 한 번에 외부 호출이 순차로 두 번 일어날 수 있어 과하게 늘리지 않는다.
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(4);

    @Bean
    public RestClientCustomizer externalApiTimeoutCustomizer() {
        return builder -> builder.requestFactory(createRequestFactory());
    }

    private SimpleClientHttpRequestFactory createRequestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return requestFactory;
    }
}
