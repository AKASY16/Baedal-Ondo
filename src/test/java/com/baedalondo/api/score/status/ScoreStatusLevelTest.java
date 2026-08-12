package com.baedalondo.api.score.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoreStatusLevelTest {

    @ParameterizedTest(name = "{0}점은 {1}")
    @CsvSource({
            "100, VERY_HIGH",
            "80,  VERY_HIGH",
            "79,  HIGH",
            "60,  HIGH",
            "59,  MEDIUM",
            "40,  MEDIUM",
            "39,  LOW",
            "20,  LOW",
            "19,  CLOSED",
            "0,   CLOSED"
    })
    @DisplayName("점수 구간 경계가 상태 코드로 정확히 나뉜다")
    void mapsScoreToLevel(int score, ScoreStatusLevel expected) {
        assertEquals(expected, ScoreStatusLevel.from(score));
    }

    @ParameterizedTest(name = "{0}점은 {1} 클래스")
    @CsvSource({
            "90, status-high",
            "70, status-high",
            "50, status-medium",
            "30, status-low",
            "10, status-closed"
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
        assertEquals("status-low", ScoreStatusLevel.from(25).getCssClass());
    }
}
