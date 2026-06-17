package com.beadalondo.api.score;

public record ScoreBreakdown(
        int score,
        int timeScore,
        int dayScore,
        int weatherScore,
        int airQualityScore,
        int interactionScore
) {
}
