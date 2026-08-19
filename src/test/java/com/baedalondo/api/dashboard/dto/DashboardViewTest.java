package com.baedalondo.api.dashboard.dto;

import com.baedalondo.api.common.ServiceTime;
import com.baedalondo.api.score.ScoreResult;
import com.baedalondo.api.score.status.ScoreStatusLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardViewTest {

    private static final int CURRENT_SCORE = 45;

    @Test
    @DisplayName("예보 시각을 화면 라벨로 바꾼다")
    void createsHourLabel() {
        LocalDateTime today = ServiceTime.today().atTime(17, 0);

        DashboardView view = createView(Map.of(today, createScoreResult(62)));

        assertEquals("17시", view.getForecastScores().get(0).getHourLabel());
    }

    @Test
    @DisplayName("자정을 넘긴 예보는 내일로 표시한다")
    void marksNextDayForecast() {
        // 밤에 조회하면 예보 5칸이 자정을 넘어간다. "0시"만 쓰면 오늘로 읽힌다.
        LocalDateTime tomorrow = ServiceTime.today().plusDays(1).atTime(0, 0);

        DashboardView view = createView(Map.of(tomorrow, createScoreResult(45)));

        assertEquals("내일 0시", view.getForecastScores().get(0).getHourLabel());
    }

    @Test
    @DisplayName("예보 순서를 그대로 유지한다")
    void keepsForecastOrder() {
        Map<LocalDateTime, ScoreResult> forecastScores = new LinkedHashMap<>();
        for (int hour = 19; hour <= 23; hour++) {
            forecastScores.put(ServiceTime.today().atTime(hour, 0), createScoreResult(hour));
        }

        DashboardView view = createView(forecastScores);

        assertEquals(
                List.of("19시", "20시", "21시", "22시", "23시"),
                view.getForecastScores().stream().map(ForecastScoreView::getHourLabel).toList()
        );
    }

    @Test
    @DisplayName("점수 구간이 예보 칸에도 그대로 적용된다")
    void appliesStatusLevelToForecast() {
        LocalDateTime at = ServiceTime.today().atTime(19, 0);

        DashboardView view = createView(Map.of(at, createScoreResult(70)));
        ForecastScoreView forecast = view.getForecastScores().get(0);

        assertEquals(ScoreStatusLevel.VERY_HIGH, forecast.getStatusLevel());
        assertEquals("status-high", forecast.getCssClass());
    }

    @Test
    @DisplayName("현재 점수 대비 증감을 함께 낸다")
    void calculatesDeltaFromCurrentScore() {
        Map<LocalDateTime, ScoreResult> forecastScores = new LinkedHashMap<>();
        forecastScores.put(ServiceTime.today().atTime(17, 0), createScoreResult(58));
        forecastScores.put(ServiceTime.today().atTime(18, 0), createScoreResult(39));
        forecastScores.put(ServiceTime.today().atTime(19, 0), createScoreResult(45));

        // createView가 현재 점수를 45로 넘긴다.
        List<ForecastScoreView> forecasts = createView(forecastScores).getForecastScores();

        assertEquals("현재 대비 +13", forecasts.get(0).getDeltaLabel());
        assertEquals("현재 대비 -6", forecasts.get(1).getDeltaLabel());
        assertEquals("현재와 같음", forecasts.get(2).getDeltaLabel());
    }

    @Test
    @DisplayName("예보가 없으면 빈 목록이 된다")
    void handlesEmptyForecast() {
        assertTrue(createView(Map.of()).getForecastScores().isEmpty());
        assertTrue(createView(null).getForecastScores().isEmpty());
    }

    private DashboardView createView(Map<LocalDateTime, ScoreResult> forecastScores) {
        return DashboardView.from(null, createScoreResult(CURRENT_SCORE), forecastScores);
    }

    private ScoreResult createScoreResult(int score) {
        return new ScoreResult(
                score,
                "보통 · 평균 수요 구간",
                "평균적인 수요가 예상됩니다.",
                "•",
                "평소와 비슷한 주문 시간대",
                "•",
                "평일 주문 흐름은 평소와 비슷한 편",
                "•",
                "외출에 큰 불편이 없는 날씨",
                "•",
                "외출에 큰 불편이 없는 대기질",
                "미세먼지 30, 초미세먼지 15"
        );
    }
}
