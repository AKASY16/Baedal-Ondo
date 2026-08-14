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

    public AccountLoginFailureHandler() {
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

        super.onAuthenticationFailure(request, response, exception);
    }
}
