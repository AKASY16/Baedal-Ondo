package com.beadalondo.api.score;

public class ScoreResult {

    private final int score;
    private final String status;
    private final String message;

    public ScoreResult(int score, String status, String message) {
        this.score = score;
        this.status = status;
        this.message = message;
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