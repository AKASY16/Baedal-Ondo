package com.baedalondo.api.score.calculator;

import com.baedalondo.api.common.ServiceTime;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;


@Component
public class KmaTimeCalculator {

    // 초단기예보는 매시 30분 발표분이 45분 전후부터 조회된다.
    private static final int FORECAST_AVAILABLE_MINUTE = 45;

    public LocalDateTime getSafeBaseDateTime() {
        return ServiceTime.now()
                .minusHours(1)
                .truncatedTo(ChronoUnit.HOURS);
    }

    /**
     * 초단기예보의 안전한 발표 시각.
     *
     * 초단기실황은 매시 정시에 관측하지만, 초단기예보는 매시 30분에 발표하고
     * 45분 전후에 제공된다. 그래서 실황용 기준 시각을 그대로 쓸 수 없다.
     *
     * 14:20 -> 13:30 발표분
     * 14:50 -> 14:30 발표분
     */
    public LocalDateTime getSafeForecastBaseDateTime() {
        LocalDateTime now = ServiceTime.now();
        LocalDateTime announced = now.truncatedTo(ChronoUnit.HOURS).withMinute(30);

        if (now.getMinute() < FORECAST_AVAILABLE_MINUTE) {
            return announced.minusHours(1);
        }

        return announced;
    }

}
