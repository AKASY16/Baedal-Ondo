package com.baedalondo.api;

import com.baedalondo.api.dashboard.dto.DashboardView;
import com.baedalondo.api.dashboard.service.DashboardService;
import com.baedalondo.api.guest.domain.GuestRegion;
import com.baedalondo.api.guest.service.GuestRegionService;
import com.baedalondo.api.score.ScoreResult;
import com.baedalondo.api.score.dto.ScoreTarget;
import com.baedalondo.api.score.service.ScoreService;
import com.baedalondo.api.store.service.StoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardGuestFallbackTest {

    @Mock
    private StoreService storeService;

    @Mock
    private ScoreService scoreService;

    @Mock
    private GuestRegionService guestRegionService;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(storeService, scoreService, guestRegionService);
    }

    @Test
    void userWithoutStoreReceivesDashboardFromGuestRegionProvider() {
        GuestRegion region = guestRegion();
        when(storeService.getCurrentLoginUserStores()).thenReturn(List.of());
        when(guestRegionService.getRandomSeoulRegion()).thenReturn(region);
        when(guestRegionService.getGuestRegion(18L)).thenReturn(region);
        when(scoreService.calculateCurrentScore(any(ScoreTarget.class))).thenReturn(scoreResult());

        DashboardView dashboard = dashboardService.getDashboard();

        assertEquals("송파구청", dashboard.getStore().getName());
        assertEquals("서울특별시 송파구 올림픽로 326", dashboard.getStore().getRoadAddress());
        assertNull(dashboard.getStore().getBusinessType());

        ArgumentCaptor<ScoreTarget> targetCaptor = ArgumentCaptor.forClass(ScoreTarget.class);
        verify(scoreService).calculateCurrentScore(targetCaptor.capture());
        assertEquals(62, targetCaptor.getValue().getNx());
        assertEquals(126, targetCaptor.getValue().getNy());
        assertNull(targetCaptor.getValue().getBusinessType());
    }

    private GuestRegion guestRegion() {
        return new GuestRegion(
                18L,
                "송파구청",
                "서울특별시 송파구 올림픽로 326 (신천동)",
                "서울특별시 송파구 올림픽로 326",
                "서울특별시 송파구 신천동 29-5 송파구청",
                "",
                "05552",
                "서울특별시",
                "송파구",
                "신천동",
                "1171010200",
                "117103123023",
                "1171010200100290005000269",
                "올림픽로",
                "0",
                "326",
                "0",
                62,
                126
        );
    }

    private ScoreResult scoreResult() {
        return new ScoreResult(
                50,
                "보통",
                "평균적인 수요가 예상됩니다.",
                "•",
                "평소와 비슷한 주문 시간대",
                "•",
                "평소와 비슷한 요일 흐름",
                "•",
                "외출에 큰 불편이 없는 날씨",
                "•",
                "외출에 큰 불편이 없는 대기질",
                "대기질 정보"
        );
    }
}
