package com.baedalondo.api.score.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KmaTimeCalculatorTest {

    private final KmaTimeCalculator calculator = new KmaTimeCalculator();

    @Test
    @DisplayName("전달받은 요청 기준시각으로 초단기예보 발표 시각을 계산한다")
    void calculatesForecastBaseTimeFromReferenceTime() {
        LocalDateTime referenceTime = LocalDateTime.of(2026, 8, 22, 14, 0, 0, 100_000_000);

        assertEquals(
                LocalDateTime.of(2026, 8, 22, 13, 30),
                calculator.getSafeForecastBaseDateTime(referenceTime)
        );
    }
}
