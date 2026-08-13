package com.baedalondo.api.auth.controller;

import com.baedalondo.api.auth.dto.SignupRequest;
import com.baedalondo.api.auth.service.SignupConflictException;
import com.baedalondo.api.auth.service.SignupService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Objects;

@Controller
public class SignupController {

    private final SignupService signupService;

    public SignupController(SignupService signupService) {
        this.signupService = signupService;
    }

    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("signupRequest", new SignupRequest());
        return "auth/signup";
    }

    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute("signupRequest") SignupRequest request,
                         BindingResult bindingResult) {
        if (!Objects.equals(request.getPassword(), request.getPasswordConfirm())) {
            bindingResult.rejectValue("passwordConfirm", "password.mismatch", "비밀번호가 서로 다릅니다.");
        }

        if (bindingResult.hasErrors()) {
            return "auth/signup";
        }

        try {
            signupService.signup(request);
        } catch (SignupConflictException e) {
            bindingResult.rejectValue(e.getField(), e.getField() + ".duplicate", e.getMessage());
            return "auth/signup";
        }

        return "redirect:/login?signup";
    }
}
