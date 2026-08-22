package com.baedalondo.api;

import com.baedalondo.api.dashboard.dto.DashboardView;
import com.baedalondo.api.dashboard.service.DashboardService;
import com.baedalondo.api.guest.domain.GuestRegion;
import com.baedalondo.api.guest.service.GuestRegionService;
import com.baedalondo.api.score.ScoreResult;
import com.baedalondo.api.score.dto.ScoreTarget;
import com.baedalondo.api.score.factory.ScoreMessageFactory;
import com.baedalondo.api.score.service.ScoreService;
import com.baedalondo.api.store.service.StoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        dashboardService = new DashboardService(
                storeService, scoreService, guestRegionService, new ScoreMessageFactory());
    }

    @Test
    void userWithoutStoreReceivesDashboardFromGuestRegionProvider() {
        GuestRegion region = guestRegion();
        when(storeService.getCurrentLoginUserStores()).thenReturn(List.of());
        when(guestRegionService.getRandomSeoulRegion()).thenReturn(region);
        when(guestRegionService.getGuestRegion(18L)).thenReturn(region);
        when(scoreService.calculateCurrentScore(
                any(ScoreTarget.class), any(LocalDateTime.class))).thenReturn(scoreResult());

        DashboardView dashboard = dashboardService.getDashboard();

        assertEquals("송파구청", dashboard.getStore().getName());
        assertEquals("서울특별시 송파구 올림픽로 326", dashboard.getStore().getRoadAddress());
        assertNull(dashboard.getStore().getBusinessType());
        assertEquals("현재 배달 수요가 평소 수준입니다.", dashboard.getMessage());

        ArgumentCaptor<ScoreTarget> targetCaptor = ArgumentCaptor.forClass(ScoreTarget.class);
        ArgumentCaptor<LocalDateTime> currentReferenceTime =
                ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> forecastReferenceTime =
                ArgumentCaptor.forClass(LocalDateTime.class);

        verify(scoreService).calculateCurrentScore(
                targetCaptor.capture(), currentReferenceTime.capture());
        verify(scoreService).calculateForecastScore(
                any(ScoreTarget.class), forecastReferenceTime.capture());
        assertEquals(62, targetCaptor.getValue().getNx());
        assertEquals(126, targetCaptor.getValue().getNy());
        assertNull(targetCaptor.getValue().getBusinessType());
        assertEquals(currentReferenceTime.getValue(), forecastReferenceTime.getValue());
    }

    @Test
    void dashboardMessageUsesOnlyTheNearestThreeForecastsInTimeOrder() {
        GuestRegion region = guestRegion();
        LocalDateTime base = LocalDateTime.of(2026, 8, 22, 13, 0);
        Map<LocalDateTime, ScoreResult> forecastScores = new LinkedHashMap<>();

        // Map 삽입 순서와 무관하게 +1~+3시간만 써야 한다. +4시간의 급등은 문구에 반영하지 않는다.
        forecastScores.put(base.plusHours(4), scoreResult(100));
        forecastScores.put(base.plusHours(2), scoreResult(58));
        forecastScores.put(base.plusHours(1), scoreResult(61));
        forecastScores.put(base.plusHours(3), scoreResult(62));

        when(guestRegionService.getGuestRegion(18L)).thenReturn(region);
        when(scoreService.calculateCurrentScore(
                any(ScoreTarget.class), any(LocalDateTime.class))).thenReturn(scoreResult(60));
        when(scoreService.calculateForecastScore(
                any(ScoreTarget.class), any(LocalDateTime.class))).thenReturn(forecastScores);

        DashboardView dashboard = dashboardService.getGuestDashboard(18L);

        assertEquals(
                "현재 배달 수요가 높은 편입니다. "
                        + "앞으로 1~3시간은 지금과 비슷한 흐름이 이어질 전망입니다.",
                dashboard.getMessage()
        );
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
        return scoreResult(50);
    }

    private ScoreResult scoreResult(int score) {
        ScoreMessageFactory messageFactory = new ScoreMessageFactory();
        return new ScoreResult(
                score,
                messageFactory.calculateStatus(score),
                messageFactory.createMessage(score),
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
