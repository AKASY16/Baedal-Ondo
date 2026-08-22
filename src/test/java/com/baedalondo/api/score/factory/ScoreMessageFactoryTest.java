package com.baedalondo.api.score.factory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ScoreMessageFactoryTest {

    private final ScoreMessageFactory messageFactory = new ScoreMessageFactory();

    @Test
    @DisplayName("현재가 매우 낮아도 1~3시간 안에 크게 오르면 피크 전망을 함께 안내한다")
    void combinesVeryLowCurrentStateWithLargeRise() {
        String message = messageFactory.createMessage(36, List.of(69, 71, 71));

        assertEquals(
                "현재 배달 수요가 매우 낮은 편입니다. "
                        + "앞으로 1~3시간 안에 배달온도가 크게 오를 전망입니다. "
                        + "피크에 대비해 미리 준비해 두세요.",
                message
        );
        assertFalse(message.contains("보수적으로"));
    }

    @Test
    @DisplayName("미래 점수 차이가 7점 이내면 비슷한 흐름으로 안내한다")
    void describesSimilarFlow() {
        assertEquals(
                "현재 배달 수요가 높은 편입니다. "
                        + "앞으로 1~3시간은 지금과 비슷한 흐름이 이어질 전망입니다.",
                messageFactory.createMessage(60, List.of(61, 58, 62))
        );
    }

    @Test
    @DisplayName("1~3시간 안에 25점 이상 낮아지면 큰 하락으로 안내한다")
    void describesLargeFall() {
        assertEquals(
                "현재 배달 수요가 매우 높은 편입니다. "
                        + "앞으로 1~3시간 안에 배달온도가 크게 낮아질 전망입니다. "
                        + "이후 추가 준비는 신중하게 가져가세요.",
                messageFactory.createMessage(70, List.of(55, 50, 46))
        );
    }

    @Test
    @DisplayName("미래 점수가 없으면 현재 상태만 안내한다")
    void keepsOnlyCurrentMessageWithoutForecasts() {
        assertEquals(
                "현재 배달 수요가 평소 수준입니다.",
                messageFactory.createMessage(50, List.of())
        );
        assertEquals(
                "현재 배달 수요가 평소 수준입니다.",
                messageFactory.createMessage(50, null)
        );
    }

    @Test
    @DisplayName("최하위 점수 구간을 영업 마감으로 표현하지 않는다")
    void describesLowestStatusWithoutClosingWording() {
        assertEquals("매우 낮음 · 한산한 수요 구간", messageFactory.calculateStatus(36));
    }

    @Test
    @DisplayName("상승과 하락이 모두 크면 절대 차이가 더 큰 방향을 사용한다")
    void selectsTheLargerDirectionWhenBothChange() {
        assertEquals(
                "앞으로 1~3시간 안에 배달온도가 눈에 띄게 낮아질 전망입니다. "
                        + "추가 준비는 신중하게 가져가세요.",
                messageFactory.createForecastMessage(50, List.of(62, 34, 51))
        );
    }
}
