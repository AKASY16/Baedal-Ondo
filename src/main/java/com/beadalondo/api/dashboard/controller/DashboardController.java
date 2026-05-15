package com.beadalondo.api.dashboard.controller;

import com.beadalondo.api.dashboard.dto.DashboardView;
import com.beadalondo.api.dashboard.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String home() {
        // TODO: 로그인 기능 구현 후
        //  - 비로그인 사용자는 redirect:/auth/login
        //  - 로그인 사용자는 redirect:/dashboard/main

        return "redirect:/dashboard/main";
    }



    @GetMapping("/dashboard/main")
    public String main(Model model) {

        DashboardView dashboard = dashboardService.getDashboard();
        model.addAttribute("dashboard", dashboard);

        return "dashboard/main";
    }

}
