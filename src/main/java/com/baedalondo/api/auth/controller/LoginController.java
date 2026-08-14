package com.baedalondo.api.auth.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import static com.baedalondo.api.auth.service.AccountLoginFailureHandler.FAILED_LOGIN_ID_SESSION_KEY;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String loginForm(HttpSession session, Model model) {
        Object failedLoginId = session.getAttribute(FAILED_LOGIN_ID_SESSION_KEY);

        if (failedLoginId instanceof String loginId) {
            model.addAttribute("loginId", loginId);
        }

        session.removeAttribute(FAILED_LOGIN_ID_SESSION_KEY);
        return "auth/login";
    }
}
