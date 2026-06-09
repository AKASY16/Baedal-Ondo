package com.beadalondo.api.dashboard.service;

import com.beadalondo.api.score.ScoreResult;
import com.beadalondo.api.score.service.ScoreService;
import com.beadalondo.api.store.domain.Store;
import com.beadalondo.api.dashboard.dto.DashboardView;
import com.beadalondo.api.store.service.StoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardService {

    private final StoreService storeService;
    private final ScoreService scoreService;

    public DashboardService(StoreService storeService, ScoreService scoreService) {
        this.storeService = storeService;
        this.scoreService = scoreService;
    }


    public DashboardView getDashboard() {
        long totalStart = System.nanoTime();
        Store store = null;

        try {
            long storeStart = System.nanoTime();
            try {
                store = storeService.getCurrentStore();
            } finally {
                logTiming("getCurrentStore", storeStart, storeId(store));
            }

            ScoreResult scoreResult;
            long scoreStart = System.nanoTime();
            try {
                scoreResult = scoreService.calculateCurrentScore(store);
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

        try {
            long storeStart = System.nanoTime();
            try {
                store = storeService.getStoreById(storeId);
            } finally {
                logTiming("getCurrentStore", storeStart, storeId(store));
            }

            ScoreResult scoreResult;
            long scoreStart = System.nanoTime();
            try {
                scoreResult = scoreService.calculateCurrentScore(store);
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

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

}
