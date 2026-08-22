package com.baedalondo.api.score.calculator;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;


@Component
public class KmaTimeCalculator {

    /**
     * 초단기예보의 기준 발표 시각.
     *
     * 예보는 매시 30분에 발표되어 45분 전후부터 조회되고, 발표 시각 +1시간부터 6시간을 준다.
     * 항상 직전 시각의 발표분을 쓰면 현재 시각이 예보 범위의 첫 항목이 된다.
     *
     * 16:00 ~ 16:59 -> 15:30 발표분 -> 16, 17, 18, 19, 20, 21시
     *
     * 45분 이후에는 더 최신 발표분이 나오지만 쓰지 않는다. 그쪽으로 넘어가면 현재 시각이
     * 예보 범위에서 빠져 매시 마지막 15분 동안 현재 점수를 낼 수 없다.
     */
    public LocalDateTime getSafeForecastBaseDateTime(LocalDateTime referenceTime) {
        return referenceTime
                .truncatedTo(ChronoUnit.HOURS)
                .minusHours(1)
                .withMinute(30);
    }

}
