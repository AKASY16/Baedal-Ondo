package com.baedalondo.api;

import com.baedalondo.api.dashboard.controller.DashboardController;
import com.baedalondo.api.dashboard.dto.DashboardView;
import com.baedalondo.api.dashboard.service.DashboardService;
import com.baedalondo.api.store.domain.Store;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;

import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    @Mock
    private HttpSession session;

    @Mock
    private Principal principal;

    @InjectMocks
    private DashboardController dashboardController;

    @Test
    void authenticatedUserWithoutStoreSeesRegistrationPromptOverGuestFallback() {
        DashboardView dashboard = mock(DashboardView.class);
        when(dashboardService.getDashboard()).thenReturn(dashboard);
        when(dashboardService.getCurrentUserStores()).thenReturn(List.of());

        ExtendedModelMap model = new ExtendedModelMap();

        String viewName = dashboardController.main(model, session, principal);

        assertEquals("dashboard/main", viewName);
        assertEquals(true, model.get("authenticated"));
        assertEquals(true, model.get("showStoreRegistrationPrompt"));
        assertEquals(List.of(), model.get("stores"));
    }

    @Test
    void authenticatedUserWithStoreDoesNotSeeRegistrationPrompt() {
        DashboardView dashboard = mock(DashboardView.class);
        Store store = mock(Store.class);
        when(dashboard.getStore()).thenReturn(store);
        when(store.getId()).thenReturn(1L);
        when(dashboardService.getDashboard()).thenReturn(dashboard);
        when(dashboardService.getCurrentUserStores()).thenReturn(List.of(store));

        ExtendedModelMap model = new ExtendedModelMap();

        dashboardController.main(model, session, principal);

        assertEquals(false, model.get("showStoreRegistrationPrompt"));
        assertEquals(1L, model.get("selectedStoreId"));
    }
}
