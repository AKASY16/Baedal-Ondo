package com.baedalondo.api.dashboard.service;

import com.baedalondo.api.dashboard.dto.DashboardView;
import com.baedalondo.api.guest.domain.GuestRegion;
import com.baedalondo.api.guest.service.GuestRegionService;
import com.baedalondo.api.score.ScoreResult;
import com.baedalondo.api.score.dto.ScoreTarget;
import com.baedalondo.api.score.service.ScoreService;
import com.baedalondo.api.store.domain.Store;
import com.baedalondo.api.store.service.StoreService;
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
        // 게스트 지역은 화면 표시를 위해 각 구청 데이터를 임시 Store로 변환하고,
        // 점수 계산에는 ScoreTarget을 사용한다.
        GuestRegion region = guestRegionService.getGuestRegion(guestRegionId);
        Store guestStore = createGuestStore(region);
        ScoreTarget scoreTarget = ScoreTarget.from(region);

        ScoreResult scoreResult = scoreService.calculateCurrentScore(scoreTarget);

        return DashboardView.from(guestStore, scoreResult);
    }

    public DashboardView getRandomGuestDashboard() {
        GuestRegion region = guestRegionService.getRandomSeoulRegion();
        return getGuestDashboard(region.getId());
    }

    public DashboardView getDashboard() {
        List<Store> storeList = storeService.getCurrentLoginUserStores();

        if (storeList.isEmpty()) {
            return getGuestFallbackDashboard();
        }

        Store store = storeList.get(0);
        ScoreTarget scoreTarget = ScoreTarget.from(store);
        ScoreResult scoreResult = scoreService.calculateCurrentScore(scoreTarget);

        return DashboardView.from(store, scoreResult);
    }

    public DashboardView getDashboardById(Long storeId) {
        Store store = storeService.getCurrentUserStoreById(storeId);
        ScoreTarget scoreTarget = ScoreTarget.from(store);
        ScoreResult scoreResult = scoreService.calculateCurrentScore(scoreTarget);

        return DashboardView.from(store, scoreResult);
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
        if (!isBlank(region.getDisplayName())) {
            return region.getDisplayName();
        }

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

    private DashboardView getGuestFallbackDashboard() {
        return getRandomGuestDashboard();
    }
}
