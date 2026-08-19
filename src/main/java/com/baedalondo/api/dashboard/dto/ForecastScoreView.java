package com.baedalondo.api.dashboard.dto;

import com.baedalondo.api.score.status.ScoreStatusLevel;

/**
 * 예보 시각 하나의 화면 표시용 값.
 *
 * 예보 칸에는 점수와 상태만 쓰므로 ScoreResult의 12개 필드를 그대로 넘기지 않는다.
 * 시각 라벨도 여기서 미리 만들어 템플릿이 날짜 포맷을 다루지 않게 한다.
 */
public class ForecastScoreView {

    private final String hourLabel;
    private final int score;
    private final ScoreStatusLevel statusLevel;

    public ForecastScoreView(String hourLabel, int score, ScoreStatusLevel statusLevel) {
        this.hourLabel = hourLabel;
        this.score = score;
        this.statusLevel = statusLevel;
    }

    /** "17시", 날짜가 넘어가면 "내일 0시" */
    public String getHourLabel() {
        return hourLabel;
    }

    public int getScore() {
        return score;
    }

    public ScoreStatusLevel getStatusLevel() {
        return statusLevel;
    }

    public String getCssClass() {
        return statusLevel.getCssClass();
    }
}
