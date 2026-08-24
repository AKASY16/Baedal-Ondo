package com.baedalondo.api;

import com.baedalondo.api.dashboard.controller.DashboardController;
import com.baedalondo.api.dashboard.dto.DashboardView;
import com.baedalondo.api.dashboard.service.DashboardService;
import com.baedalondo.api.guest.domain.GuestRegion;
import com.baedalondo.api.guest.service.GuestRegionService;
import com.baedalondo.api.store.domain.Store;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    @Mock
    private GuestRegionService guestRegionService;

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

    @Test
    void storeSelectionRedirectKeepsRegistrationNoticeAsFlashAttribute() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String viewName = dashboardController.main(7L, true, session, redirectAttributes);

        assertEquals("redirect:/dashboard/main", viewName);
        assertEquals(true, redirectAttributes.getFlashAttributes().get("registered"));
        verify(session).setAttribute("selectedStoreId", 7L);
    }

    @Test
    void guestRegionSelectionLoadsTheRequestedRegionAndListsAllChoices() {
        GuestRegion selectedRegion = mock(GuestRegion.class);
        GuestRegion anotherRegion = mock(GuestRegion.class);
        DashboardView dashboard = mock(DashboardView.class);
        when(selectedRegion.getId()).thenReturn(15L);
        when(guestRegionService.getGuestRegion(15L)).thenReturn(selectedRegion);
        when(guestRegionService.getRegions()).thenReturn(List.of(selectedRegion, anotherRegion));
        when(dashboardService.getGuestDashboard(15L)).thenReturn(dashboard);

        ExtendedModelMap model = new ExtendedModelMap();

        String viewName = dashboardController.guestDashboard(15L, session, model);

        assertEquals("dashboard/main", viewName);
        assertEquals(dashboard, model.get("dashboard"));
        assertEquals(true, model.get("guestMode"));
        assertEquals(false, model.get("authenticated"));
        assertEquals(List.of(selectedRegion, anotherRegion), model.get("guestRegions"));
        assertEquals(15L, model.get("selectedGuestRegionId"));
        verify(dashboardService).getGuestDashboard(15L);
        verify(session).removeAttribute("selectedStoreId");
    }

    @Test
    void firstGuestVisitKeepsRandomRegionAsTheDefaultSelection() {
        GuestRegion randomRegion = mock(GuestRegion.class);
        DashboardView dashboard = mock(DashboardView.class);
        when(randomRegion.getId()).thenReturn(18L);
        when(guestRegionService.getRandomSeoulRegion()).thenReturn(randomRegion);
        when(guestRegionService.getRegions()).thenReturn(List.of(randomRegion));
        when(dashboardService.getGuestDashboard(18L)).thenReturn(dashboard);

        ExtendedModelMap model = new ExtendedModelMap();

        String viewName = dashboardController.guestDashboard(null, session, model);

        assertEquals("dashboard/main", viewName);
        assertEquals(18L, model.get("selectedGuestRegionId"));
        verify(dashboardService).getGuestDashboard(18L);
    }
}
