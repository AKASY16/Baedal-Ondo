package com.baedalondo.api.score.timeweight;

import java.time.LocalTime;

/** 서울시 추정매출 데이터가 제공하는 6개 시간 구간. */
public enum TimeBand {
    TIME_00_06(0, 6),
    TIME_06_11(6, 11),
    TIME_11_14(11, 14),
    TIME_14_17(14, 17),
    TIME_17_21(17, 21),
    TIME_21_24(21, 24);

    private final int startHour;
    private final int endHour;

    TimeBand(int startHour, int endHour) {
        this.startHour = startHour;
        this.endHour = endHour;
    }

    public static TimeBand from(LocalTime time) {
        if (time == null) {
            throw new IllegalArgumentException("time은 null일 수 없습니다.");
        }

        int hour = time.getHour();
        for (TimeBand band : values()) {
            if (hour >= band.startHour && hour < band.endHour) {
                return band;
            }
        }

        throw new IllegalStateException("시간대 구간을 찾을 수 없습니다. time=" + time);
    }
}
