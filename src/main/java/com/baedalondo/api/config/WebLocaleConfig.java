package com.baedalondo.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;

import java.util.Locale;

@Configuration
public class WebLocaleConfig {

    @Bean
    public LocaleResolver localeResolver() {
        // 서비스와 서버 렌더링 문서가 모두 한국어이므로 브라우저 기본 언어에 흔들리지 않게 한다.
        return new FixedLocaleResolver(Locale.KOREA);
    }
}
