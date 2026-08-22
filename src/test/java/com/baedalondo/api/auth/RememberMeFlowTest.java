package com.baedalondo.api.auth;

import com.baedalondo.api.support.MySqlTestSupport;
import com.baedalondo.api.user.domain.UserAccount;
import com.baedalondo.api.user.repository.UserAccountRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RememberMeFlowTest extends MySqlTestSupport {

    private static final String LOGIN_ID = "remember-me-owner";
    private static final String PASSWORD = "remember-me-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void createUser() {
        deleteUser();
        userAccountRepository.save(new UserAccount(
                LOGIN_ID,
                "remember-me-owner@example.com",
                passwordEncoder.encode(PASSWORD),
                "ROLE_USER"
        ));
    }

    @AfterEach
    void deleteUser() {
        userAccountRepository.findByLoginId(LOGIN_ID).ifPresent(userAccountRepository::delete);
    }

    @Test
    @DisplayName("로그인 유지 쿠키는 30일 동안 인증하고 로그아웃하면 삭제된다")
    void rememberMeCookieAuthenticatesAndIsDeletedOnLogout() throws Exception {
        MvcResult loginPage = mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andReturn();

        CsrfToken loginCsrf = csrfToken(loginPage);
        MockHttpSession loginSession = session(loginPage);

        MvcResult login = mockMvc.perform(post("/login")
                        .session(loginSession)
                        .param(loginCsrf.getParameterName(), loginCsrf.getToken())
                        .param("loginId", LOGIN_ID)
                        .param("password", PASSWORD)
                        .param("remember-me", "on"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/main"))
                .andReturn();

        Cookie rememberMeCookie = login.getResponse().getCookie("remember-me");
        assertNotNull(rememberMeCookie, "로그인 유지 쿠키가 발급되지 않았다");
        assertEquals(30 * 24 * 60 * 60, rememberMeCookie.getMaxAge());

        MvcResult rememberedRequest = mockMvc.perform(get("/dashboard/main")
                        .cookie(rememberMeCookie))
                .andExpect(status().isOk())
                .andReturn();

        CsrfToken logoutCsrf = csrfToken(rememberedRequest);
        MockHttpSession rememberedSession = session(rememberedRequest);

        MvcResult logout = mockMvc.perform(post("/logout")
                        .session(rememberedSession)
                        .cookie(rememberMeCookie)
                        .param(logoutCsrf.getParameterName(), logoutCsrf.getToken()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"))
                .andReturn();

        List<String> setCookieHeaders = logout.getResponse().getHeaders("Set-Cookie");
        assertTrue(setCookieHeaders.stream().anyMatch(header ->
                        header.startsWith("remember-me=") && header.contains("Max-Age=0")),
                "로그아웃 응답이 remember-me 쿠키를 삭제하지 않았다");
    }

    private static CsrfToken csrfToken(MvcResult result) {
        Object csrf = result.getRequest().getAttribute(CsrfToken.class.getName());
        if (!(csrf instanceof CsrfToken)) {
            csrf = result.getRequest().getAttribute("_csrf");
        }
        assertTrue(csrf instanceof CsrfToken, "CSRF 토큰이 요청에 없다");
        return (CsrfToken) csrf;
    }

    private static MockHttpSession session(MvcResult result) {
        assertTrue(result.getRequest().getSession(false) instanceof MockHttpSession,
                "MockHttpSession이 생성되지 않았다");
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
