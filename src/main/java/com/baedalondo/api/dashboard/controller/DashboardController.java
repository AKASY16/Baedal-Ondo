package com.baedalondo.api.dashboard.controller;

import com.baedalondo.api.dashboard.dto.DashboardView;
import com.baedalondo.api.dashboard.service.DashboardService;
import com.baedalondo.api.store.domain.Store;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
public class DashboardController {
    private static final String SELECTED_STORE_ID_SESSION_KEY = "selectedStoreId";

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String home() {
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
    public String main(@PathVariable("storeId") Long storeId,
                       @RequestParam(name = "registered", defaultValue = "false") boolean registered,
                       HttpSession session,
                       RedirectAttributes redirectAttributes) {
        session.setAttribute(SELECTED_STORE_ID_SESSION_KEY, storeId);

        if (registered) {
            redirectAttributes.addFlashAttribute("registered", true);
        }

        return "redirect:/dashboard/main";
    }

    private void addDashboardModel(Model model, DashboardView dashboard, Principal principal) {
        List<Store> stores = dashboardService.getCurrentUserStores();
        boolean authenticated = principal != null;
        boolean hasNoRegisteredStore = stores.isEmpty();

        model.addAttribute("dashboard", dashboard);
        model.addAttribute("stores", stores);
        model.addAttribute("selectedStoreId",
                dashboard.getStore() == null ? null : dashboard.getStore().getId());
        model.addAttribute("authenticated", authenticated);
        model.addAttribute("showStoreRegistrationPrompt", authenticated && hasNoRegisteredStore);
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
