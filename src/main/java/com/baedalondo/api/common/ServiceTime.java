package com.baedalondo.api.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * 서비스 기준 시각.
 *
 * 배달온도는 서울 상권을 대상으로 하고 기상청, 에어코리아, 공휴일 데이터가 모두 한국 시간을
 * 기준으로 발표된다. 서버가 어느 시간대로 뜨든 계산과 기록은 한국 시간이어야 한다.
 *
 * JVM 기본 시간대를 쓰면 UTC로 동작하는 서버나 CI에서 날짜가 하루 어긋난다.
 * 시간대가 필요한 곳은 여기를 거친다.
 */
public final class ServiceTime {

    public static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private ServiceTime() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(KOREA_ZONE);
    }

    public static LocalDate today() {
        return LocalDate.now(KOREA_ZONE);
    }

    public static LocalTime currentTime() {
        return LocalTime.now(KOREA_ZONE);
    }
}
