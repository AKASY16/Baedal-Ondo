package com.baedalondo.api.score.status;

/**
 * 배달온도 점수 구간.
 *
 * 화면에 표시하는 문구와 분리된 상태 코드다.
 * 문구는 언제든 바뀔 수 있으므로 뷰는 문구가 아니라 이 값으로 스타일을 결정한다.
 *
 * VERY_HIGH와 HIGH는 현재 같은 색을 쓴다. 두 구간을 다른 색으로 나누려면
 * 여기 cssClass만 바꾸면 된다.
 */
public enum ScoreStatusLevel {

    VERY_HIGH("status-high"),
    HIGH("status-high"),
    MEDIUM("status-medium"),
    LOW("status-low"),
    CLOSED("status-closed");

    private final String cssClass;

    ScoreStatusLevel(String cssClass) {
        this.cssClass = cssClass;
    }

    public static ScoreStatusLevel from(int score) {
        if (score >= 80) {
            return VERY_HIGH;
        }

        if (score >= 60) {
            return HIGH;
        }

        if (score >= 40) {
            return MEDIUM;
        }

        if (score >= 20) {
            return LOW;
        }

        return CLOSED;
    }

    public String getCssClass() {
        return cssClass;
    }
}
