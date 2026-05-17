package com.beadalondo.api.dashboard.service;

import com.beadalondo.api.score.ScoreResult;
import com.beadalondo.api.score.service.ScoreService;
import com.beadalondo.api.store.domain.Store;
import com.beadalondo.api.dashboard.dto.DashboardView;
import com.beadalondo.api.store.service.StoreService;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final StoreService storeService;
    private final ScoreService scoreService;

    public DashboardService(StoreService storeService, ScoreService scoreService) {
        this.storeService = storeService;
        this.scoreService = scoreService;
    }


    public DashboardView getDashboard() {
        Store store = storeService.getCurrentStore();
        ScoreResult scoreResult = scoreService.calculateCurrentScore(store);

        return DashboardView.from(store, scoreResult);
    }


}
