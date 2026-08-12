package com.baedalondo.api.score.timeweight;

import com.baedalondo.api.score.status.TimeDemandLevel;
import com.baedalondo.api.store.domain.BusinessType;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TimeWeightProviderTest {

    private static final String AREA = "3120029";

    private final TimeWeightProvider provider = new TimeWeightProvider(
            Map.of(new LocalTimeWeightKey(
                            AREA, BusinessType.CAFE_BEVERAGE, TimeBand.TIME_21_24),
                    TimeDemandLevel.CLOSED),
            Map.of(
                    new CityTimeWeightKey(BusinessType.CAFE_BEVERAGE, TimeBand.TIME_21_24),
                    TimeDemandLevel.LOW,
                    new CityTimeWeightKey(BusinessType.CHICKEN, TimeBand.TIME_21_24),
                    TimeDemandLevel.VERY_HIGH)
    );

    @Test
    void localLevelTakesPrecedence() {
        assertEquals(TimeDemandLevel.CLOSED,
                provider.findDemandLevel(AREA, BusinessType.CAFE_BEVERAGE, LocalTime.of(22, 43)));
    }

    @Test
    void missingLocalFallsBackToBusinessTypeCityLevel() {
        assertEquals(TimeDemandLevel.LOW,
                provider.findDemandLevel("unknown", BusinessType.CAFE_BEVERAGE, LocalTime.of(22, 43)));
        assertEquals(TimeDemandLevel.VERY_HIGH,
                provider.findDemandLevel(AREA, BusinessType.CHICKEN, LocalTime.of(22, 43)));
    }

    @Test
    void missingBusinessTypeReturnsNullForLegacyFallback() {
        assertNull(provider.findDemandLevel(AREA, null, LocalTime.of(22, 43)));
    }
}
