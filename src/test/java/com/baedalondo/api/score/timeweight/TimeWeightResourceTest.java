package com.baedalondo.api.score.timeweight;

import com.baedalondo.api.score.status.TimeDemandLevel;
import com.baedalondo.api.store.domain.BusinessType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TimeWeightResourceTest {

    private static TimeWeightProvider provider;

    @BeforeAll
    static void loadOnce() {
        provider = new TimeWeightProvider(new TimeWeightCsvLoader());
    }

    @Test
    void loadsAllPreprocessedKeys() {
        assertEquals(27_774, provider.localKeyCount());
        assertEquals(BusinessType.values().length * TimeBand.values().length,
                provider.cityKeyCount());
    }

    @Test
    void everyBusinessTypeAndTimeBandHasCityLevel() {
        for (BusinessType businessType : BusinessType.values()) {
            for (TimeBand timeBand : TimeBand.values()) {
                int hour = switch (timeBand) {
                    case TIME_00_06 -> 1;
                    case TIME_06_11 -> 7;
                    case TIME_11_14 -> 12;
                    case TIME_14_17 -> 15;
                    case TIME_17_21 -> 18;
                    case TIME_21_24 -> 22;
                };
                assertNotNull(provider.findDemandLevel(
                        null, businessType, LocalTime.of(hour, 0)));
            }
        }
    }

    @Test
    void lateNightCafeAndChickenUseDifferentCityPatterns() {
        LocalTime lateEvening = LocalTime.of(22, 43);

        assertEquals(TimeDemandLevel.LOW,
                provider.findDemandLevel(null, BusinessType.CAFE_BEVERAGE, lateEvening));
        assertEquals(TimeDemandLevel.VERY_HIGH,
                provider.findDemandLevel(null, BusinessType.CHICKEN, lateEvening));
    }
}
