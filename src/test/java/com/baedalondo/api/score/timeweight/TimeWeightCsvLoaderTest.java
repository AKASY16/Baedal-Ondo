package com.baedalondo.api.score.timeweight;

import com.baedalondo.api.score.status.TimeDemandLevel;
import com.baedalondo.api.store.domain.BusinessType;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimeWeightCsvLoaderTest {

    private static final String LOCAL_HEADER =
            "commercial_area_code,business_type,time_band,demand_level\n";
    private static final String CITY_HEADER =
            "business_type,time_band,demand_level\n";

    private final TimeWeightCsvLoader loader = new TimeWeightCsvLoader();

    @Test
    void loadsValidLocalCsv() {
        Map<LocalTimeWeightKey, TimeDemandLevel> levels = loader.loadLocal(csv(
                LOCAL_HEADER + "3120029,CAFE_BEVERAGE,TIME_21_24,LOW\n"));

        assertEquals(TimeDemandLevel.LOW, levels.get(new LocalTimeWeightKey(
                "3120029", BusinessType.CAFE_BEVERAGE, TimeBand.TIME_21_24)));
    }

    @Test
    void loadsCompleteCityCsv() {
        Map<CityTimeWeightKey, TimeDemandLevel> levels = loader.loadCity(csv(validCityCsv()));

        assertEquals(BusinessType.values().length * TimeBand.values().length, levels.size());
    }

    @Test
    void rejectsDuplicateAndUnknownValues() {
        assertThrows(IllegalStateException.class, () -> loader.loadLocal(csv(
                LOCAL_HEADER
                        + "3120029,CAFE_BEVERAGE,TIME_21_24,LOW\n"
                        + "3120029,CAFE_BEVERAGE,TIME_21_24,HIGH\n")));
        assertThrows(IllegalStateException.class, () -> loader.loadLocal(csv(
                LOCAL_HEADER + "3120029,CAFE_BEVERAGE,LATE_NIGHT,LOW\n")));
        assertThrows(IllegalStateException.class, () -> loader.loadLocal(csv(
                LOCAL_HEADER + "3120029,CAFE_BEVERAGE,TIME_21_24,EXTREME\n")));
    }

    @Test
    void rejectsIncompleteCityCsv() {
        assertThrows(IllegalStateException.class, () -> loader.loadCity(csv(
                CITY_HEADER + "CAFE_BEVERAGE,TIME_21_24,LOW\n")));
    }

    private static String validCityCsv() {
        StringBuilder result = new StringBuilder(CITY_HEADER);
        for (BusinessType businessType : BusinessType.values()) {
            for (TimeBand timeBand : TimeBand.values()) {
                result.append(businessType).append(',')
                        .append(timeBand).append(",MEDIUM\n");
            }
        }
        return result.toString();
    }

    private static Resource csv(String content) {
        return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getDescription() {
                return "test-time-weight-csv";
            }
        };
    }
}
