package com.baedalondo.api.score.timeweight;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeBandTest {

    @Test
    void mapsBoundaryTimesToSeoulDataBands() {
        assertEquals(TimeBand.TIME_00_06, TimeBand.from(LocalTime.of(0, 0)));
        assertEquals(TimeBand.TIME_00_06, TimeBand.from(LocalTime.of(5, 59)));
        assertEquals(TimeBand.TIME_06_11, TimeBand.from(LocalTime.of(6, 0)));
        assertEquals(TimeBand.TIME_11_14, TimeBand.from(LocalTime.of(11, 0)));
        assertEquals(TimeBand.TIME_14_17, TimeBand.from(LocalTime.of(14, 0)));
        assertEquals(TimeBand.TIME_17_21, TimeBand.from(LocalTime.of(17, 0)));
        assertEquals(TimeBand.TIME_21_24, TimeBand.from(LocalTime.of(21, 0)));
        assertEquals(TimeBand.TIME_21_24, TimeBand.from(LocalTime.of(23, 59)));
    }
}
