package com.baedalondo.api.auth.service;

import com.baedalondo.api.user.service.UserAccountActivityService;
import jakarta.servlet.ServletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AccountLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(AccountLoginSuccessHandler.class);

    private final UserAccountActivityService userAccountActivityService;
    private final LoginAttemptGuard loginAttemptGuard;

    public AccountLoginSuccessHandler(UserAccountActivityService userAccountActivityService,
                                      LoginAttemptGuard loginAttemptGuard) {
        this.userAccountActivityService = userAccountActivityService;
        this.loginAttemptGuard = loginAttemptGuard;
        setDefaultTargetUrl("/dashboard/main");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        // 비밀번호를 몇 번 헷갈렸다가 맞춘 사람에게 실패가 다음 방문까지 따라다니지 않게 한다.
        loginAttemptGuard.recordSuccess(authentication.getName());

        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            // 로그인 이력은 부가 기록이다. 저장에 실패해도 인증은 이미 성공했으므로
            // 예외를 필터 체인으로 올리지 않고 로그만 남긴 뒤 로그인을 진행한다.
            try {
                userAccountActivityService.recordSuccessfulLogin(userDetails.getUserId());
            } catch (RuntimeException exception) {
                log.warn("로그인 이력 기록에 실패했습니다. userId={}", userDetails.getUserId(), exception);
            }
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }
}
