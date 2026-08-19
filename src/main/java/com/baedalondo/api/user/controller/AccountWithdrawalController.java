package com.baedalondo.api.user.controller;

import com.baedalondo.api.auth.service.CurrentUserService;
import com.baedalondo.api.user.service.AccountWithdrawalService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AccountWithdrawalController {

    private final AccountWithdrawalService accountWithdrawalService;
    private final CurrentUserService currentUserService;

    public AccountWithdrawalController(AccountWithdrawalService accountWithdrawalService,
                                       CurrentUserService currentUserService) {
        this.accountWithdrawalService = accountWithdrawalService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/account/withdraw")
    public String withdrawPage() {
        return "account/withdraw";
    }

    /**
     탈퇴를 처리하고 세션을 끊는다.

     삭제가 끝난 뒤에도 세션이 남아 있으면 이후 요청이 존재하지 않는 계정으로 들어와
     조회에서 터진다. 삭제와 로그아웃은 한 흐름으로 처리한다.
     */
    @PostMapping("/account/withdraw")
    public String withdraw(@RequestParam("password") String password,
                           HttpServletRequest request,
                           HttpServletResponse response,
                           Model model) {
        Long userId = currentUserService.getCurrentUserId();

        try {
            accountWithdrawalService.withdraw(userId, password);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "account/withdraw";
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        new SecurityContextLogoutHandler().logout(request, response, authentication);

        return "redirect:/login?withdrawn";
    }
}
