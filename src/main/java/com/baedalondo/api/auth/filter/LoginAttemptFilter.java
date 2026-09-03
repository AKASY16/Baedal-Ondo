package com.baedalondo.api.auth.filter;

import com.baedalondo.api.auth.service.LoginAttemptGuard;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 로그인과 회원가입 POST를 인증 필터 앞에서 걸러낸다.

 여기서 막으면 BCrypt 대조도 DB 조회도 시작하지 않는다. 인증이 끝난 뒤에 세는 방식으로는
 이미 비싼 일을 다 하고 나서 세게 되므로 서버를 지키는 효과가 없다.

 CSRF 필터가 이 필터보다 앞이라 토큰 없이 던지는 요청은 여기까지 오지 않고 403으로 끝난다.
 그 경로는 세션 조회만 하고 끝나 비용이 거의 없다. 물량 자체를 막는 것은 앱이 아니라
 Nginx나 Cloudflare가 할 일이다.

 스프링이 이 필터를 서블릿 컨테이너에도 등록하지 않도록 빈으로 두지 않는다.
 SecurityConfig가 직접 만들어 시큐리티 체인에만 넣는다.
 */
public class LoginAttemptFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/login";
    private static final String SIGNUP_PATH = "/signup";
    private static final String LOGIN_ID_PARAMETER = "loginId";
    private static final String BLOCKED_QUERY = "?blocked";

    private final LoginAttemptGuard loginAttemptGuard;

    public LoginAttemptFilter(LoginAttemptGuard loginAttemptGuard) {
        this.loginAttemptGuard = loginAttemptGuard;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (!isGuardedSubmission(request, path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 시도 자체는 잠긴 계정이든 아니든 먼저 센다. 잠긴 계정을 계속 두드리는 것도 시도다.
        boolean tooManyAttempts = loginAttemptGuard.tooManyAttempts(clientAddress(request));
        boolean lockedAccount = LOGIN_PATH.equals(path)
                && loginAttemptGuard.isLocked(request.getParameter(LOGIN_ID_PARAMETER));

        if (tooManyAttempts || lockedAccount) {
            // 폼 화면이라 429 대신 원래 화면으로 돌려보낸다. 실수로 걸린 사람이 이유를 봐야 한다.
            // JSON을 주는 경로였다면 429와 Retry-After가 맞다.
            response.sendRedirect(path + BLOCKED_QUERY);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isGuardedSubmission(HttpServletRequest request, String path) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }

        return LOGIN_PATH.equals(path) || SIGNUP_PATH.equals(path);
    }

    /**
     forward-headers-strategy: native 설정 덕분에 톰캣이 X-Forwarded-For를 읽어
     getRemoteAddr에 넣어 준다. 다만 Nginx가 Cloudflare 뒤에서 실제 접속자 IP를 넘기지
     않으면 여기 들어오는 값은 Cloudflare 엣지 주소가 된다. 그래도 계정 기준 잠금은
     그대로 동작하고, 주소 기준 제한은 정상 사용자가 닿지 않을 만큼 넉넉하게 잡아 뒀다.
     */
    private String clientAddress(HttpServletRequest request) {
        String address = request.getRemoteAddr();

        return address == null || address.isBlank() ? "unknown" : address;
    }
}
