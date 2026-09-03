package com.baedalondo.api.auth.service;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AccountLoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    public static final String FAILED_LOGIN_ID_SESSION_KEY = "failedLoginId";

    private final LoginAttemptGuard loginAttemptGuard;

    public AccountLoginFailureHandler(LoginAttemptGuard loginAttemptGuard) {
        this.loginAttemptGuard = loginAttemptGuard;
        setDefaultFailureUrl("/login?error");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String loginId = request.getParameter("loginId");

        if (loginId != null) {
            request.getSession().setAttribute(FAILED_LOGIN_ID_SESSION_KEY, loginId);
        }

        // 계정이 없어서 실패했는지 비밀번호가 틀려서 실패했는지 여기서는 구분하지 않는다.
        // 구분해서 세면 잠기는지 여부가 그 아이디가 있다는 신호가 된다.
        loginAttemptGuard.recordFailure(loginId);

        super.onAuthenticationFailure(request, response, exception);
    }
}
