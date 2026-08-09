package com.baedalondo.api.score;

public record ScoreCalculationResult(
        int score,
        int timeScore,
        int dayScore,
        int weatherScore,
        int airQualityScore,
        int interactionScore
) {
}
