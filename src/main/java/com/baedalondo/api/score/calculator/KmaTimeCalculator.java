package com.baedalondo.api.score.calculator;

import com.baedalondo.api.common.ServiceTime;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;


@Component
public class KmaTimeCalculator {

    public LocalDateTime getSafeBaseDateTime() {
        return ServiceTime.now()
                .minusHours(1)
                .truncatedTo(ChronoUnit.HOURS);
    }

}
