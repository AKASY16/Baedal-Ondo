package com.baedalondo.api.score.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoreStatusLevelTest {

    @ParameterizedTest(name = "{0}점은 {1}")
    @CsvSource({
            "100, VERY_HIGH",
            "64,  VERY_HIGH",
            "63,  HIGH",
            "56,  HIGH",
            "55,  MEDIUM",
            "42,  MEDIUM",
            "41,  LOW",
            "37,  LOW",
            "36,  CLOSED",
            "0,   CLOSED"
    })
    @DisplayName("점수 구간 경계가 상태 코드로 정확히 나뉜다")
    void mapsScoreToLevel(int score, ScoreStatusLevel expected) {
        assertEquals(expected, ScoreStatusLevel.from(score));
    }

    @ParameterizedTest(name = "{0}점은 {1} 클래스")
    @CsvSource({
            "90, status-high",
            "60, status-high",
            "50, status-medium",
            "38, status-low",
            "32, status-closed"
    })
    @DisplayName("화면 스타일 클래스가 점수 구간을 따라간다")
    void mapsScoreToCssClass(int score, String expected) {
        assertEquals(expected, ScoreStatusLevel.from(score).getCssClass());
    }

    @Test
    @DisplayName("모든 구간이 status-closed로 뭉개지지 않는다")
    void doesNotCollapseToClosed() {
        // 표시 문구가 바뀌었을 때 모든 점수가 회색으로 떨어졌던 회귀를 막는다.
        assertEquals("status-high", ScoreStatusLevel.from(85).getCssClass());
        assertEquals("status-medium", ScoreStatusLevel.from(45).getCssClass());
        assertEquals("status-low", ScoreStatusLevel.from(38).getCssClass());
    }

    @Test
    @DisplayName("실제로 나올 수 있는 점수 범위에서 모든 구간이 등장한다")
    void coversEveryLevelWithinReachableRange() {
        // WeightedScoreCalculator의 하한은 32다. 경계를 20점 단위로 두면
        // CLOSED가 한 번도 나오지 않고 맑은 날에는 VERY_HIGH도 나오지 않았다.
        EnumSet<ScoreStatusLevel> reached = EnumSet.noneOf(ScoreStatusLevel.class);
        for (int score = 32; score <= 100; score++) {
            reached.add(ScoreStatusLevel.from(score));
        }

        assertEquals(EnumSet.allOf(ScoreStatusLevel.class), reached);
    }

    @Test
    @DisplayName("맑은 날 상한 73으로도 최상위 구간에 도달한다")
    void reachesTopLevelWithoutBadWeather() {
        // 시간대 +14, 요일 +6, 상호작용 +3이 전부 최대여도 날씨가 0이면 73이 천장이다.
        assertEquals(ScoreStatusLevel.VERY_HIGH, ScoreStatusLevel.from(73));
    }
}
