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
    private final int delta;

    public ForecastScoreView(String hourLabel, int score, ScoreStatusLevel statusLevel, int delta) {
        this.hourLabel = hourLabel;
        this.score = score;
        this.statusLevel = statusLevel;
        this.delta = delta;
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

    /** 현재 점수 대비 증감. 카드가 작아 부호와 숫자만 쓴다. */
    public int getDelta() {
        return delta;
    }

    /** 카드에 그대로 찍는 문구. 0일 때 "현재 대비 0"은 어색해서 따로 쓴다. */
    public String getDeltaLabel() {
        if (delta > 0) {
            return "현재 대비 +" + delta;
        }

        if (delta < 0) {
            return "현재 대비 " + delta;
        }

        return "현재와 같음";
    }
}
