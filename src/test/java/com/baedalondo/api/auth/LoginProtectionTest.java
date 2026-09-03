package com.baedalondo.api.auth;

import com.baedalondo.api.support.MySqlTestSupport;
import com.baedalondo.api.user.domain.UserAccount;
import com.baedalondo.api.user.repository.UserAccountRepository;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 로그인과 회원가입 시도 제한이 실제 필터 체인에서 동작하는지 확인한다.

 제한 값을 여기서만 낮춘다. 운영 값(주소당 10회, 계정당 5회)으로 확인하려면 요청을 그만큼
 보내야 하고, 그러면 이 클래스가 다른 테스트의 횟수까지 같이 쓰게 된다.
 값을 바꾸면 컨텍스트가 따로 뜨므로 시도 기록도 이 클래스 안에서만 쌓인다.

 테스트마다 접속 주소를 다르게 준다. 같은 주소를 쓰면 앞 테스트가 쓴 횟수가 남아
 뒤 테스트가 처음부터 막힌 상태로 시작한다.
 **/
@SpringBootTest(properties = {
        "baedalondo.login-protection.max-attempts-per-address=3",
        "baedalondo.login-protection.max-failures-per-account=2"
})
@AutoConfigureMockMvc
class LoginProtectionTest extends MySqlTestSupport {

    private static final String LOGIN_ID = "protection-owner";
    private static final String PASSWORD = "protection-password";

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
                "protection-owner@example.com",
                passwordEncoder.encode(PASSWORD),
                "ROLE_USER"
        ));
    }

    @AfterEach
    void deleteUser() {
        userAccountRepository.findByLoginId(LOGIN_ID).ifPresent(userAccountRepository::delete);
    }

    @Test
    @DisplayName("계정을 바꿔가며 시도해도 같은 주소에서 오면 막힌다")
    void blocksRepeatedAttemptsFromOneAddress() throws Exception {
        String address = "203.0.113.20";
        MvcResult loginPage = openLoginPage();

        // 계정마다 실패가 한 번씩이라 계정 잠금에는 걸리지 않는다.
        // 유출된 아이디와 비밀번호 쌍을 여러 계정에 한 번씩 넣어보는 모양이다.
        for (int attempt = 1; attempt <= 3; attempt++) {
            submitLogin(loginPage, address, "stuffing-" + attempt, "wrong-password")
                    .andExpect(redirectedUrl("/login?error"));
        }

        submitLogin(loginPage, address, "stuffing-4", "wrong-password")
                .andExpect(redirectedUrl("/login?blocked"));
    }

    @Test
    @DisplayName("실패가 쌓인 계정은 올바른 비밀번호로도 들어가지 못한다")
    void blocksLockedAccountEvenWithTheCorrectPassword() throws Exception {
        String address = "203.0.113.21";
        MvcResult loginPage = openLoginPage();

        submitLogin(loginPage, address, LOGIN_ID, "wrong-password")
                .andExpect(redirectedUrl("/login?error"));
        submitLogin(loginPage, address, LOGIN_ID, "wrong-password")
                .andExpect(redirectedUrl("/login?error"));

        // 주소 기준으로는 아직 여유가 있다. 여기서 막히면 계정이 잠긴 것이다.
        submitLogin(loginPage, address, LOGIN_ID, PASSWORD)
                .andExpect(redirectedUrl("/login?blocked"));
    }

    @Test
    @DisplayName("회원가입 폼도 같은 주소 제한을 받는다")
    void blocksRepeatedSignupSubmissions() throws Exception {
        String address = "203.0.113.22";
        MvcResult signupPage = openSignupPage();
        CsrfToken csrf = csrfToken(signupPage);
        MockHttpSession session = session(signupPage);

        // 값을 채우지 않아 검증에서 되돌아온다. 계정이 실제로 만들어지지는 않는다.
        for (int attempt = 1; attempt <= 3; attempt++) {
            mockMvc.perform(post("/signup")
                            .with(from(address))
                            .session(session)
                            .param(csrf.getParameterName(), csrf.getToken()))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/signup")
                        .with(from(address))
                        .session(session)
                        .param(csrf.getParameterName(), csrf.getToken()))
                .andExpect(redirectedUrl("/signup?blocked"));
    }

    @Test
    @DisplayName("막힌 뒤에 열리는 로그인 화면은 이유를 알려준다")
    void showsTheReasonOnTheLoginPage() throws Exception {
        MvcResult blockedPage = mockMvc.perform(get("/login").param("blocked", ""))
                .andExpect(status().isOk())
                .andReturn();

        assertTrue(blockedPage.getResponse().getContentAsString().contains("잠시 로그인을 제한했습니다"),
                "제한 안내가 로그인 화면에 없다");
    }

    private MvcResult openLoginPage() throws Exception {
        return mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andReturn();
    }

    private MvcResult openSignupPage() throws Exception {
        return mockMvc.perform(get("/signup"))
                .andExpect(status().isOk())
                .andReturn();
    }

    private ResultActions submitLogin(MvcResult loginPage,
                                      String address,
                                      String loginId,
                                      String password) throws Exception {
        CsrfToken csrf = csrfToken(loginPage);

        return mockMvc.perform(post("/login")
                .with(from(address))
                .session(session(loginPage))
                .param(csrf.getParameterName(), csrf.getToken())
                .param("loginId", loginId)
                .param("password", password));
    }

    /**
     MockMvc는 접속 주소를 127.0.0.1로 두므로 그대로 쓰면 모든 테스트가 한 칸을 나눠 쓴다.
     */
    private static RequestPostProcessor from(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
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
