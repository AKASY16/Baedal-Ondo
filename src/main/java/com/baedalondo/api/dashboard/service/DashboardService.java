package com.baedalondo.api.dashboard.service;

import com.baedalondo.api.dashboard.dto.DashboardView;
import com.baedalondo.api.guest.domain.GuestRegion;
import com.baedalondo.api.guest.service.GuestRegionService;
import com.baedalondo.api.score.ScoreResult;
import com.baedalondo.api.score.dto.ScoreTarget;
import com.baedalondo.api.score.service.ScoreService;
import com.baedalondo.api.store.domain.Store;
import com.baedalondo.api.store.service.StoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardService {

    private final StoreService storeService;
    private final ScoreService scoreService;
    private final GuestRegionService guestRegionService;

    public DashboardService(StoreService storeService,
                            ScoreService scoreService,
                            GuestRegionService guestRegionService) {
        this.storeService = storeService;
        this.scoreService = scoreService;
        this.guestRegionService = guestRegionService;
    }

    public DashboardView getGuestDashboard(Long guestRegionId) {
        long totalStart = System.nanoTime();

        // 게스트 지역은 화면 표시를 위해 각 구청 데이터를 임시 Store로 변환하고,
        // 점수 계산에는 ScoreTarget을 사용한다.

        try {
            GuestRegion region = guestRegionService.getGuestRegion(guestRegionId);
            Store guestStore = createGuestStore(region);
            ScoreTarget scoreTarget = ScoreTarget.from(region);

            ScoreResult scoreResult;
            long scoreStart = System.nanoTime();
            try {
                scoreResult = scoreService.calculateCurrentScore(scoreTarget);
            } finally {
                logTiming("calculateGuestScore", scoreStart, guestRegionId);
            }

            return DashboardView.from(guestStore, scoreResult);
        } finally {
            log.info("dashboard timing step=guestDashboardTotal elapsedMs={} guestRegionId={}",
                    elapsedMs(totalStart),
                    guestRegionId);
        }
    }

    public DashboardView getDashboard() {
        long totalStart = System.nanoTime();
        Store store = null;
        List<Store> storeList = null;
        ScoreTarget scoreTarget = null;

        try {
            long storeStart = System.nanoTime();

            try {
                storeList = storeService.getCurrentLoginUserStores();

                if(storeList.isEmpty()){
                    return getGuestFallbackDashboard();
                }

                store = storeList.get(0);

                scoreTarget = ScoreTarget.from(store);
            } finally {
                logTiming("getCurrentStore", storeStart, storeId(store));
            }

            ScoreResult scoreResult;
            long scoreStart = System.nanoTime();

            try {
                scoreResult = scoreService.calculateCurrentScore(scoreTarget);
            } finally {
                logTiming("calculateCurrentScore", scoreStart, storeId(store));
            }

            long viewStart = System.nanoTime();
            try {
                return DashboardView.from(store, scoreResult);
            } finally {
                logTiming("dashboardView", viewStart, storeId(store));
            }
        } finally {
            logTiming("dashboardTotal", totalStart, storeId(store));
        }
    }

    public DashboardView getDashboardById(Long storeId) {
        long totalStart = System.nanoTime();
        Store store = null;
        ScoreTarget scoreTarget = null;

        try {
            long storeStart = System.nanoTime();
            try {
                store = storeService.getCurrentUserStoreById(storeId);
                scoreTarget = ScoreTarget.from(store);
            } finally {
                logTiming("getCurrentStore", storeStart, storeId(store));
            }

            ScoreResult scoreResult;
            long scoreStart = System.nanoTime();
            try {
                scoreResult = scoreService.calculateCurrentScore(scoreTarget);
            } finally {
                logTiming("calculateCurrentScore", scoreStart, storeId(store));
            }

            long viewStart = System.nanoTime();
            try {
                return DashboardView.from(store, scoreResult);
            } finally {
                logTiming("dashboardView", viewStart, storeId(store));
            }
        } finally {
            logTiming("dashboardTotal", totalStart, storeId(store));
        }
    }

    public List<Store> getStores() {
        return storeService.getStores();
    }

    public List<Store> getCurrentUserStores() {
        return storeService.getCurrentLoginUserStores();
    }

    private Store createGuestStore(GuestRegion region) {
        if (region == null) {
            throw new IllegalArgumentException("GuestRegion is required.");
        }

        if (region.getNx() == null || region.getNy() == null) {
            throw new IllegalStateException("GuestRegion weather grid coordinate is required.");
        }

        return new Store(
                createGuestStoreName(region),
                null, // 게스트 지역은 특정 업종의 매장이 아니라 저장되지도 않는다.
                firstNonBlank(region.getAddress(), region.getRoadAddress()),
                region.getRoadAddress(),
                region.getJibunAddress(),
                region.getAddressDetail(),
                region.getPostalCode(),
                normalizeAirKoreaSidoName(region.getSidoName()),
                region.getSigunguName(),
                region.getDongName(),
                region.getAddressRegionCode(),
                region.getRoadNameCode(),
                region.getBuildingManagementNumber(),
                region.getRoadName(),
                region.getUndergroundYn(),
                region.getBuildingMainNumber(),
                region.getBuildingSubNumber(),
                region.getNx(),
                region.getNy()
        );
    }

    private String createGuestStoreName(GuestRegion region) {
        String sidoName = region.getSidoName();
        String sigunguName = region.getSigunguName();

        if (isBlank(sidoName) && isBlank(sigunguName)) {
            return "게스트 지역";
        }

        if (isBlank(sidoName)) {
            return sigunguName;
        }

        if (isBlank(sigunguName)) {
            return sidoName;
        }

        return sidoName + " " + sigunguName;
    }

    private String firstNonBlank(String first, String second) {
        return isBlank(first) ? second : first;
    }

    private String normalizeAirKoreaSidoName(String sidoName) {
        if (isBlank(sidoName)) {
            return sidoName;
        }

        return switch (sidoName.trim()) {
            case "서울특별시", "서울시" -> "서울";
            case "부산광역시", "부산시" -> "부산";
            case "대구광역시", "대구시" -> "대구";
            case "인천광역시", "인천시" -> "인천";
            case "광주광역시", "광주시" -> "광주";
            case "대전광역시", "대전시" -> "대전";
            case "울산광역시", "울산시" -> "울산";
            case "세종특별자치시", "세종시" -> "세종";
            case "경기도" -> "경기";
            case "강원특별자치도", "강원도" -> "강원";
            case "충청북도" -> "충북";
            case "충청남도" -> "충남";
            case "전북특별자치도", "전라북도" -> "전북";
            case "전라남도" -> "전남";
            case "경상북도" -> "경북";
            case "경상남도" -> "경남";
            case "제주특별자치도", "제주도" -> "제주";
            default -> sidoName.trim();
        };
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void logTiming(String step, long startNanos, Long storeId) {
        log.info("dashboard timing step={} elapsedMs={} storeId={}",
                step,
                elapsedMs(startNanos),
                storeId);
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private Long storeId(Store store) {
        return store == null ? null : store.getId();
    }

    private DashboardView getGuestFallbackDashboard() {
        GuestRegion region = guestRegionService.getRandomSeoulRegion();
        return getGuestDashboard(region.getId());
    }

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);
}
