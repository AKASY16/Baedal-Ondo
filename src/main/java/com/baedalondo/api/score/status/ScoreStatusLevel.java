package com.baedalondo.api.score.status;

/**
 * 배달온도 점수 구간.
 *
 * 화면에 표시하는 문구와 분리된 상태 코드다.
 * 문구는 언제든 바뀔 수 있으므로 뷰는 문구가 아니라 이 값으로 스타일을 결정한다.
 *
 * VERY_HIGH와 HIGH는 현재 같은 색을 쓴다. 두 구간을 다른 색으로 나누려면
 * 여기 cssClass만 바꾸면 된다.
 *
 * 구간 경계는 20점 단위가 아니라 실측 분포에서 뽑았다.
 * data-processing/simulate_score_distribution.py 참고.
 *
 * WeightedScoreCalculator가 낼 수 있는 점수는 32 ~ 100이다.
 * BASE 50에 시간대 -12~+14, 요일 -6~+6, 날씨 0~+20, 대기질 0~+8,
 * 상호작용 0~+10이 더해지는 구조라 하한이 0까지 내려가지 않는다.
 * 20점 단위로 자르면 0~19 구간은 어떤 입력으로도 나오지 않고,
 * 맑은 날 상한이 73이라 80~100 구간도 악천후에만 나왔다.
 *
 * 그래서 맑은 날 분포를 기준선으로 잡고 경계를 다시 뽑았다.
 * 악천후는 실제로 수요를 올리는 요인이므로 위 구간으로 밀어올리는 게 맞고,
 * 기준선은 평범한 날이어야 하기 때문이다.
 *
 * 맑은 날 점유율: VERY_HIGH 12.6 / HIGH 18.7 / MEDIUM 41.0 / LOW 19.4 / CLOSED 8.4 (%)
 */
public enum ScoreStatusLevel {

    VERY_HIGH("status-high"),
    HIGH("status-high"),
    MEDIUM("status-medium"),
    LOW("status-low"),
    CLOSED("status-closed");

    private static final int VERY_HIGH_THRESHOLD = 64;
    private static final int HIGH_THRESHOLD = 56;
    private static final int MEDIUM_THRESHOLD = 42;
    private static final int LOW_THRESHOLD = 37;

    private final String cssClass;

    ScoreStatusLevel(String cssClass) {
        this.cssClass = cssClass;
    }

    public static ScoreStatusLevel from(int score) {
        if (score >= VERY_HIGH_THRESHOLD) {
            return VERY_HIGH;
        }

        if (score >= HIGH_THRESHOLD) {
            return HIGH;
        }

        if (score >= MEDIUM_THRESHOLD) {
            return MEDIUM;
        }

        if (score >= LOW_THRESHOLD) {
            return LOW;
        }

        return CLOSED;
    }

    public String getCssClass() {
        return cssClass;
    }
}
