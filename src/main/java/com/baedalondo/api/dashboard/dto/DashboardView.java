package com.baedalondo.api.dashboard.dto;

import com.baedalondo.api.common.ServiceTime;
import com.baedalondo.api.score.ScoreResult;
import com.baedalondo.api.score.status.ScoreStatusLevel;
import com.baedalondo.api.store.domain.Store;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class DashboardView {
    private final Store store;
    private final int score;
    private final String status;
    private final String message;
    private final String timeFactor;
    private final String timeDescription;
    private final String dayFactor;
    private final String dayDescription;
    private final String currentWeatherFactor;
    private final String currentWeatherDescription;
    private final String airQualityFactor;
    private final String airQualityDescription;
    private final String airQualityDetail;
    private final List<ForecastScoreView> forecastScores;

    public DashboardView(Store store,
                         int score,
                         String status,
                         String message,
                         String timeFactor,
                         String timeDescription,
                         String dayFactor,
                         String dayDescription,
                         String currentWeatherFactor,
                         String currentWeatherDescription,
                         String airQualityFactor,
                         String airQualityDescription,
                         String airQualityDetail,
                         List<ForecastScoreView> forecastScores) {
        this.store = store;
        this.score = score;
        this.status = status;
        this.message = message;
        this.timeFactor = timeFactor;
        this.timeDescription = timeDescription;
        this.dayFactor = dayFactor;
        this.dayDescription = dayDescription;
        this.currentWeatherFactor = currentWeatherFactor;
        this.currentWeatherDescription = currentWeatherDescription;
        this.airQualityFactor = airQualityFactor;
        this.airQualityDescription = airQualityDescription;
        this.airQualityDetail = airQualityDetail;
        this.forecastScores = forecastScores;
    }

    public static DashboardView from(
            Store store,
            ScoreResult scoreResult,
            Map<LocalDateTime, ScoreResult> forecastScores
    ) {
        return new DashboardView(
                store,
                scoreResult.getScore(),
                scoreResult.getStatus(),
                scoreResult.getMessage(),
                scoreResult.getTimeFactor(),
                scoreResult.getTimeDescription(),
                scoreResult.getDayFactor(),
                scoreResult.getDayDescription(),
                scoreResult.getCurrentWeatherFactor(),
                scoreResult.getCurrentWeatherDescription(),
                scoreResult.getAirQualityFactor(),
                scoreResult.getAirQualityDescription(),
                scoreResult.getAirQualityDetail(),
                toForecastViews(forecastScores)
        );
    }


    public List<ForecastScoreView> getForecastScores() {
        return forecastScores;
    }

    /**
     예보 Map을 화면이 그대로 쓸 수 있는 형태로 바꾼다.
     시각 라벨을 여기서 만들어 템플릿이 날짜 포맷과 자정 넘김을 다루지 않게 한다.
     */
    private static List<ForecastScoreView> toForecastViews(
            Map<LocalDateTime, ScoreResult> forecastScores) {
        if (forecastScores == null || forecastScores.isEmpty()) {
            return List.of();
        }

        LocalDate today = ServiceTime.today();

        return forecastScores.entrySet().stream()
                .map(entry -> new ForecastScoreView(
                        createHourLabel(entry.getKey(), today),
                        entry.getValue().getScore(),
                        entry.getValue().getStatusLevel()
                ))
                .toList();
    }

    private static String createHourLabel(LocalDateTime forecastAt, LocalDate today) {
        String hour = forecastAt.getHour() + "시";

        // 밤에 조회하면 예보 5칸이 자정을 넘어간다. 그때 "0시"만 쓰면 오늘로 읽힌다.
        if (forecastAt.toLocalDate().isAfter(today)) {
            return "내일 " + hour;
        }

        return hour;
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

    /**
     * 화면 스타일 결정용 점수 구간 코드.
     * 템플릿이 status 문구를 파싱하지 않도록 별도로 노출한다.
     */
    public ScoreStatusLevel getStatusLevel() {
        return ScoreStatusLevel.from(score);
    }

    public String getMessage() {
        return message;
    }

    public String getDayFactor() {
        return dayFactor;
    }

    public String getTimeFactor() {
        return timeFactor;
    }

    public String getDayDescription() {
        return dayDescription;
    }

    public String getTimeDescription() {
        return timeDescription;
    }

    public String getCurrentWeatherFactor() {
        return currentWeatherFactor;
    }

    public String getCurrentWeatherDescription() {
        return currentWeatherDescription;
    }

    public String getAirQualityFactor() {
        return airQualityFactor;
    }

    public String getAirQualityDescription() {
        return airQualityDescription;
    }

    public String getAirQualityDetail() {
        return airQualityDetail;
    }
}
