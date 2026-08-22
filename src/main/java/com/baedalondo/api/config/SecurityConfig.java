package com.baedalondo.api.config;

import com.baedalondo.api.auth.service.AccountLoginFailureHandler;
import com.baedalondo.api.auth.service.AccountLoginSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           AccountLoginSuccessHandler loginSuccessHandler,
                                           AccountLoginFailureHandler loginFailureHandler) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login",
                                "/signup",
                                "/terms",
                                "/privacy",
                                "/actuator/health",
                                // 오류 페이지는 ERROR 디스패치로 다시 들어오므로 열어두지 않으면
                                // 비로그인 사용자에게 404 대신 로그인 리다이렉트가 나간다.
                                "/error",
                                "/guest",
                                "/dashboard/guest",
                                // 크롤러는 로그인하지 않는다. 열어두지 않으면 로그인으로 튕겨
                                // 색인 규칙과 사이트맵이 전달되지 않는다.
                                "/robots.txt",
                                "/sitemap.xml",
                                "/css/**",
                                "/fonts/**",
                                "/js/**",
                                "/images/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("loginId")
                        .passwordParameter("password")
                        .successHandler(loginSuccessHandler)
                        .failureHandler(loginFailureHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                "/store/register",
                                "/store/register/**",
                                "/store/*/edit"
                        )
                )
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                );

        return http.build();
    }
}
