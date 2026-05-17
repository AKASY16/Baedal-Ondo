package com.beadalondo.api.score.calculator;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;


@Component
public class KmaTimeCalculator {

    public LocalDateTime getSafeBaseDateTime() {
        return LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                .minusHours(1)
                .truncatedTo(ChronoUnit.HOURS);
    }

}
