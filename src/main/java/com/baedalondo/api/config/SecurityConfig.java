package com.baedalondo.api.config;

import com.baedalondo.api.auth.filter.LoginAttemptFilter;
import com.baedalondo.api.auth.service.AccountLoginFailureHandler;
import com.baedalondo.api.auth.service.AccountLoginSuccessHandler;
import com.baedalondo.api.auth.service.LoginAttemptGuard;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Arrays;

@Configuration
public class SecurityConfig {

    private static final String[] AUTHENTICATED_PATHS = {
            "/dashboard/**",
            "/store/**",
            "/api/**",
            "/account/**",
            "/logout"
    };

    private static final RequestMatcher LOGIN_REDIRECT_REQUESTS = new OrRequestMatcher(
            Arrays.stream(AUTHENTICATED_PATHS)
                    .<RequestMatcher>map(PathPatternRequestMatcher::pathPattern)
                    .toList()
    );

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           AccountLoginSuccessHandler loginSuccessHandler,
                                           AccountLoginFailureHandler loginFailureHandler,
                                           LoginAttemptGuard loginAttemptGuard,
                                           @Value("${baedalondo.security.remember-me-key}") String rememberMeKey) throws Exception {
        if (rememberMeKey.length() < 32) {
            throw new IllegalArgumentException("REMEMBER_ME_KEY는 32자 이상의 랜덤 문자열이어야 합니다.");
        }

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
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
                        // 공개 게스트 화면을 먼저 허용한 뒤 기능 단위 경로 전체를 보호한다.
                        // 새 하위 URL이 생겨도 각 화면을 일일이 matcher에 추가하지 않아도 된다.
                        .requestMatchers(AUTHENTICATED_PATHS).authenticated()
                        // health 이외의 actuator 경로가 나중에 노출 설정에 추가돼도 외부에는 열지 않는다.
                        .requestMatchers("/actuator/**").denyAll()
                        .anyRequest().authenticated()
                )
                // 인증 필터 앞에 둔다. 여기서 막으면 BCrypt 대조와 DB 조회를 시작하지 않는다.
                .addFilterBefore(new LoginAttemptFilter(loginAttemptGuard),
                        UsernamePasswordAuthenticationFilter.class)
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("loginId")
                        .passwordParameter("password")
                        .successHandler(loginSuccessHandler)
                        .failureHandler(loginFailureHandler)
                        .permitAll()
                )
                .rememberMe(rememberMe -> rememberMe
                        .key(rememberMeKey)
                        .rememberMeParameter("remember-me")
                        .tokenValiditySeconds(90 * 24 * 60 * 60)
                )
                // 로그인 성공 후 항상 대시보드로 이동하므로 보호 요청을 세션에 저장하지 않는다.
                // 잘못된 URL을 요청한 크롤러에게도 불필요한 JSESSIONID가 생기지 않게 한다.
                .requestCache(cache -> cache
                        .requestCache(new NullRequestCache())
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "remember-me")
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
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(notFoundUnlessLoginProtected())
                );

        return http.build();
    }

    private AuthenticationEntryPoint notFoundUnlessLoginProtected() {
        LoginUrlAuthenticationEntryPoint loginEntryPoint =
                new LoginUrlAuthenticationEntryPoint("/login");

        return (request, response, authenticationException) -> {
            if (LOGIN_REDIRECT_REQUESTS.matches(request)) {
                loginEntryPoint.commence(request, response, authenticationException);
                return;
            }

            // 공개 목록에도 보호 경로에도 없는 URL은 새 공개 기능으로 추측하지 않는다.
            // 익명 사용자에게도 로그인 화면 대신 실제 404를 반환하면서 기본 정책은 fail-closed로 둔다.
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        };
    }
}
