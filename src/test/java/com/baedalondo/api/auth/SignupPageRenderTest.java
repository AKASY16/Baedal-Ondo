package com.baedalondo.api.auth;

import com.baedalondo.api.support.MySqlTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 세션 없는 첫 방문자가 회원가입 페이지를 온전히 받는지 확인한다.

 Thymeleaf는 기본값으로 렌더하면서 결과를 바로 흘려보낸다. 페이지가 톰캣 응답 버퍼(8KB)를
 넘으면 그 지점에서 응답이 커밋되고, 뒤에 나오는 th:action이 CSRF 토큰을 넣으려 세션을
 만들려다 "Cannot create a session after the response has been committed"로 터진다.
 회원가입 폼은 8KB 뒤에 있어 실제로 이 문제가 있었다.

 세션이 이미 있으면 세션을 만들 필요가 없어 멀쩡하게 동작한다. 그래서 개발 중에는
 드러나지 않고, 아무것도 없는 상태로 처음 들어온 사람만 페이지가 잘린다.

 MockMvc로는 재현되지 않는다. 실제 톰캣과 실제 버퍼가 있어야 한다.
 **/
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SignupPageRenderTest extends MySqlTestSupport {

    @LocalServerPort
    private int port;

    @Autowired
    private RestClient.Builder restClientBuilder;

    @Test
    @DisplayName("세션 없이 처음 들어와도 회원가입 페이지가 끝까지 내려온다")
    void rendersSignupPageForFirstTimeVisitor() {
        String html = get("/signup");

        assertTrue(html.trim().endsWith("</html>"), "페이지가 중간에 잘렸다");
        assertTrue(html.contains("name=\"_csrf\""), "CSRF 토큰이 없다. 가입 요청이 403으로 막힌다");
        assertTrue(html.contains("action=\"/signup\""), "폼이 렌더되지 않았다");
    }

    @Test
    @DisplayName("세션 없이 처음 들어와도 로그인 페이지가 끝까지 내려온다")
    void rendersLoginPageForFirstTimeVisitor() {
        String html = get("/login");

        assertTrue(html.trim().endsWith("</html>"), "페이지가 중간에 잘렸다");
        assertTrue(html.contains("name=\"_csrf\""), "CSRF 토큰이 없다");
        assertTrue(html.contains("id=\"saveLoginId\""), "아이디 저장 체크박스가 없다");
        assertTrue(html.contains("name=\"remember-me\""), "로그인 유지 체크박스가 없다");
        assertTrue(html.contains("localStorage.setItem(storageKey, loginIdInput.value)"),
                "아이디 저장 스크립트가 없다");
        assertTrue(!html.contains("localStorage.setItem(storageKey, password"),
                "비밀번호를 localStorage에 저장하면 안 된다");
    }

    private String get(String path) {
        // 쿠키를 들고 다니지 않는 새 클라이언트여야 세션 없는 첫 방문이 된다.
        ResponseEntity<String> response = restClientBuilder.build()
                .get()
                .uri("http://localhost:" + port + path)
                .retrieve()
                .toEntity(String.class);

        String body = response.getBody();
        assertNotNull(body, "응답 본문이 비었다");

        return body;
    }
}
