package com.baedalondo.api;

import com.baedalondo.api.auth.controller.LoginController;
import com.baedalondo.api.auth.service.AccountLoginFailureHandler;
import com.baedalondo.api.auth.service.LoginAttemptGuard;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.ui.ExtendedModelMap;

import java.time.Duration;
import java.time.Instant;

import static com.baedalondo.api.auth.service.AccountLoginFailureHandler.FAILED_LOGIN_ID_SESSION_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LoginFailureFlowTest {

    @Test
    void failedLoginPreservesOnlyLoginIdAndRedirectsToErrorPage() throws Exception {
        AccountLoginFailureHandler handler = new AccountLoginFailureHandler(new LoginAttemptGuard(
                10,
                Duration.ofMinutes(1),
                5,
                Duration.ofMinutes(10),
                Instant::now
        ));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setParameter("loginId", "owner01");
        request.setParameter("password", "wrong-password");

        handler.onAuthenticationFailure(
                request,
                response,
                new BadCredentialsException("Bad credentials")
        );

        assertEquals("/login?error", response.getRedirectedUrl());
        assertEquals("owner01", request.getSession().getAttribute(FAILED_LOGIN_ID_SESSION_KEY));
        assertNull(request.getSession().getAttribute("password"));
    }

    @Test
    void loginPageConsumesFailedLoginIdFromSession() {
        LoginController controller = new LoginController();
        MockHttpSession session = new MockHttpSession();
        ExtendedModelMap model = new ExtendedModelMap();
        session.setAttribute(FAILED_LOGIN_ID_SESSION_KEY, "owner01");

        String viewName = controller.loginForm(session, model);

        assertEquals("auth/login", viewName);
        assertEquals("owner01", model.get("loginId"));
        assertNull(session.getAttribute(FAILED_LOGIN_ID_SESSION_KEY));
    }
}
