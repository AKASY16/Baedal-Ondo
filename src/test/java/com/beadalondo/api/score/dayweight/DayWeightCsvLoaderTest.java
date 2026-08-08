package com.beadalondo.api.score.dayweight;

import com.beadalondo.api.store.domain.BusinessType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DayWeightCsvLoaderTest {

    private static final String LOCAL_HEADER =
            "commercial_area_code,business_type,day_of_week,weight\n";
    private static final String CITY_HEADER =
            "business_type,day_of_week,weight\n";

    private final DayWeightCsvLoader loader = new DayWeightCsvLoader();

    private static Resource csv(String content) {
        return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getDescription() {
                return "test-csv";
            }
        };
    }

    /** 지원 업종 x 7요일을 모두 채운 정상 City CSV. */
    private static String validCityCsv() {
        StringBuilder sb = new StringBuilder(CITY_HEADER);
        for (BusinessType businessType : BusinessType.values()) {
            for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
                sb.append(businessType).append(',').append(dayOfWeek).append(",1\n");
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------ 정상 로딩

    @Test
    @DisplayName("정상 Local CSV를 읽는다")
    void loadsValidLocal() {
        Map<LocalDayWeightKey, Integer> weights = loader.loadLocal(csv(LOCAL_HEADER
                + "3120029,KOREAN_FOOD,MONDAY,1\n"
                + "3120029,KOREAN_FOOD,SUNDAY,-5\n"));

        assertEquals(2, weights.size());
        assertEquals(1, weights.get(
                new LocalDayWeightKey("3120029", BusinessType.KOREAN_FOOD, DayOfWeek.MONDAY)));
        assertEquals(-5, weights.get(
                new LocalDayWeightKey("3120029", BusinessType.KOREAN_FOOD, DayOfWeek.SUNDAY)));
    }

    @Test
    @DisplayName("정상 City CSV를 읽는다")
    void loadsValidCity() {
        Map<CityDayWeightKey, Integer> weights = loader.loadCity(csv(validCityCsv()));

        assertEquals(BusinessType.values().length * 7, weights.size());
        assertEquals(1, weights.get(
                new CityDayWeightKey(BusinessType.CHICKEN, DayOfWeek.FRIDAY)));
    }

    // ------------------------------------------------------------ 검증 실패

    @Test
    @DisplayName("중복 Local key면 로딩에 실패한다")
    void failsOnDuplicateLocalKey() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> loader.loadLocal(csv(LOCAL_HEADER
                        + "3120029,KOREAN_FOOD,MONDAY,1\n"
                        + "3120029,KOREAN_FOOD,MONDAY,2\n")));

        assertTrue(e.getMessage().contains("중복된 Local key"), e.getMessage());
    }

    @Test
    @DisplayName("중복 City key면 로딩에 실패한다")
    void failsOnDuplicateCityKey() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> loader.loadCity(csv(validCityCsv() + "CHICKEN,FRIDAY,2\n")));

        assertTrue(e.getMessage().contains("중복된 City key"), e.getMessage());
    }

    @Test
    @DisplayName("알 수 없는 business_type이면 로딩에 실패한다")
    void failsOnUnknownBusinessType() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> loader.loadLocal(csv(LOCAL_HEADER + "3120029,PIZZA_ONLY,MONDAY,1\n")));

        assertTrue(e.getMessage().contains("business_type"), e.getMessage());
    }

    @Test
    @DisplayName("알 수 없는 day_of_week이면 로딩에 실패한다")
    void failsOnUnknownDayOfWeek() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> loader.loadLocal(csv(LOCAL_HEADER + "3120029,KOREAN_FOOD,월요일,1\n")));

        assertTrue(e.getMessage().contains("day_of_week"), e.getMessage());
    }

    @Test
    @DisplayName("weight가 -6 ~ +6 범위를 벗어나면 로딩에 실패한다")
    void failsOnWeightOutOfRange() {
        assertThrows(IllegalStateException.class,
                () -> loader.loadLocal(csv(LOCAL_HEADER + "3120029,KOREAN_FOOD,MONDAY,7\n")));
        assertThrows(IllegalStateException.class,
                () -> loader.loadLocal(csv(LOCAL_HEADER + "3120029,KOREAN_FOOD,MONDAY,-7\n")));
    }

    @Test
    @DisplayName("weight가 정수가 아니면 로딩에 실패한다")
    void failsOnNonIntegerWeight() {
        assertThrows(IllegalStateException.class,
                () -> loader.loadLocal(csv(LOCAL_HEADER + "3120029,KOREAN_FOOD,MONDAY,1.5\n")));
    }

    @Test
    @DisplayName("commercial_area_code가 비어 있으면 로딩에 실패한다")
    void failsOnBlankAreaCode() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> loader.loadLocal(csv(LOCAL_HEADER + ",KOREAN_FOOD,MONDAY,1\n")));

        assertTrue(e.getMessage().contains("commercial_area_code"), e.getMessage());
    }

    @Test
    @DisplayName("City key가 63개가 아니면 로딩에 실패한다")
    void failsWhenCityKeyCountIsWrong() {
        String missingOneRow = validCityCsv()
                .replace("BAKERY,SUNDAY,1\n", "");

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> loader.loadCity(csv(missingOneRow)));

        assertTrue(e.getMessage().contains("key 개수"), e.getMessage());
    }

    @Test
    @DisplayName("헤더가 다르면 로딩에 실패한다")
    void failsOnWrongHeader() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> loader.loadLocal(csv("area_code,business_type,day_of_week,weight\n"
                        + "3120029,KOREAN_FOOD,MONDAY,1\n")));

        assertTrue(e.getMessage().contains("헤더"), e.getMessage());
    }

    @Test
    @DisplayName("컬럼 수가 맞지 않으면 로딩에 실패한다")
    void failsOnFieldCountMismatch() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> loader.loadLocal(csv(LOCAL_HEADER + "3120029,KOREAN_FOOD,MONDAY\n")));

        assertTrue(e.getMessage().contains("컬럼 수"), e.getMessage());
    }

    @Test
    @DisplayName("Local이 비어 있으면 로딩에 실패한다")
    void failsOnEmptyLocal() {
        assertThrows(IllegalStateException.class, () -> loader.loadLocal(csv(LOCAL_HEADER)));
    }

    @Test
    @DisplayName("리소스가 없으면 로딩에 실패한다")
    void failsOnMissingResource() {
        assertThrows(IllegalStateException.class,
                () -> loader.loadLocal(new org.springframework.core.io.ClassPathResource("없는파일.csv")));
    }
}
