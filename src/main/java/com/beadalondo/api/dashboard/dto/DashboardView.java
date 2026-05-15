package com.beadalondo.api.dashboard.dto;

import com.beadalondo.api.store.domain.Store;

public class DashboardView {
    private Store store;
    private int score;
    private String status;
    private String message;

    public DashboardView(Store store, int score, String status, String message) {
        this.store = store;
        this.score = score;
        this.status = status;
        this.message = message;
    }

    public Store getStore() {
        return store;
    }

    public int getScore() {
        return score;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
