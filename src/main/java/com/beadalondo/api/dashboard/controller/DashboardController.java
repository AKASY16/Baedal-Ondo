package com.beadalondo.api.dashboard.controller;

import com.beadalondo.api.dashboard.dto.DashboardView;
import com.beadalondo.api.dashboard.service.DashboardService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class DashboardController {
    private static final String SELECTED_STORE_ID_SESSION_KEY = "selectedStoreId";

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
    public String main(Model model, HttpSession session) {

        DashboardView dashboard = getDashboard(session);
        addDashboardModel(model, dashboard);

        return "dashboard/main";
    }


    @GetMapping("/dashboard/main/{storeId}")
    public String main(@PathVariable("storeId") Long storeId, HttpSession session) {
        session.setAttribute(SELECTED_STORE_ID_SESSION_KEY, storeId);

        return "redirect:/dashboard/main";
    }

    private void addDashboardModel(Model model, DashboardView dashboard) {
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("stores", dashboardService.getStores());
        model.addAttribute("selectedStoreId", dashboard.getStore().getId());
    }

    private DashboardView getDashboard(HttpSession session) {
        Object selectedStoreId = session.getAttribute(SELECTED_STORE_ID_SESSION_KEY);

        if (selectedStoreId instanceof Long storeId) {
            return dashboardService.getDashboardById(storeId);
        }

        return dashboardService.getDashboard();
    }

}
