package com.baedalondo.api.auth.service;

import com.baedalondo.api.user.service.UserAccountActivityService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AccountLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserAccountActivityService userAccountActivityService;

    public AccountLoginSuccessHandler(UserAccountActivityService userAccountActivityService) {
        this.userAccountActivityService = userAccountActivityService;
        setDefaultTargetUrl("/dashboard/main");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            userAccountActivityService.recordSuccessfulLogin(userDetails.getUserId());
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }
}
