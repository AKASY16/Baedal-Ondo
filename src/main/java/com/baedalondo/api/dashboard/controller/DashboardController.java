package com.baedalondo.api.dashboard.controller;

import com.baedalondo.api.dashboard.dto.DashboardView;
import com.baedalondo.api.dashboard.service.DashboardService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.security.Principal;

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
    public String main(Model model, HttpSession session, Principal principal) {

        DashboardView dashboard = getDashboard(session);
        addDashboardModel(model, dashboard, principal);

        return "dashboard/main";
    }

    @GetMapping("/guest")
    public String guestMode(HttpSession session) {
        session.removeAttribute(SELECTED_STORE_ID_SESSION_KEY);

        return "redirect:/dashboard/guest";
    }

    @GetMapping("/dashboard/guest")
    public String guestDashboard(HttpSession session, Model model) {
        session.removeAttribute(SELECTED_STORE_ID_SESSION_KEY);
        DashboardView dashboard = dashboardService.getRandomGuestDashboard();

        model.addAttribute("dashboard", dashboard);
        model.addAttribute("guestMode", true);
        model.addAttribute("authenticated", false);

        return "dashboard/main";
    }


    @GetMapping("/dashboard/main/{storeId}")
    public String main(@PathVariable("storeId") Long storeId, HttpSession session) {
        session.setAttribute(SELECTED_STORE_ID_SESSION_KEY, storeId);

        return "redirect:/dashboard/main";
    }

    private void addDashboardModel(Model model, DashboardView dashboard, Principal principal) {
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("stores", dashboardService.getCurrentUserStores());
        model.addAttribute("selectedStoreId",
                dashboard.getStore() == null ? null : dashboard.getStore().getId());
        model.addAttribute("authenticated", principal != null);
    }

    private DashboardView getDashboard(HttpSession session) {
        Object selectedStoreId = session.getAttribute(SELECTED_STORE_ID_SESSION_KEY);

        if (selectedStoreId instanceof Long storeId) {
            try {
                return dashboardService.getDashboardById(storeId);
            } catch (IllegalArgumentException e) {
                session.removeAttribute(SELECTED_STORE_ID_SESSION_KEY);
            }
        }

        return dashboardService.getDashboard();
    }

}
