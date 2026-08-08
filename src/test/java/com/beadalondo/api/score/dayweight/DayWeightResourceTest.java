package com.beadalondo.api.score.dayweight;

import com.beadalondo.api.store.domain.BusinessType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 실제 배포되는 classpath 리소스를 대상으로 하는 테스트.
 전처리 결과가 런타임에서 그대로 읽히는지 확인한다.
 **/
class DayWeightResourceTest {

    private static DayWeightProvider provider;

    @BeforeAll
    static void loadOnce() {
        provider = new DayWeightProvider(new DayWeightCsvLoader());
    }

    @Test
    @DisplayName("전처리 결과 Local 32,403건이 모두 로딩된다")
    void loadsAllLocalKeys() {
        assertEquals(32_403, provider.localKeyCount());
    }

    @Test
    @DisplayName("City는 지원 업종 x 7요일 = 63건이 로딩된다")
    void loadsAllCityKeys() {
        assertEquals(63, provider.cityKeyCount());
        assertEquals(BusinessType.values().length * 7, provider.cityKeyCount());
    }

    @Test
    @DisplayName("모든 업종 x 요일에 대해 City 조회가 가능하다")
    void everyBusinessTypeHasCityWeight() {
        for (BusinessType businessType : BusinessType.values()) {
            for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
                int weight = provider.findWeight(null, businessType, dayOfWeek);
                assertTrue(weight >= -6 && weight <= 6,
                        "weight 범위를 벗어났습니다. " + businessType + " " + dayOfWeek + " = " + weight);
            }
        }
    }

    @Test
    @DisplayName("실제 Local 데이터가 City보다 우선한다")
    void realLocalOverridesCity() {
        // 여의도역 커피-음료는 일요일 DayIndex 21.8 -> Local weight -6,
        // City CAFE_BEVERAGE 일요일은 -5 이므로 Local이 우선해야 한다.
        int local = provider.findWeight("3120149", BusinessType.CAFE_BEVERAGE, DayOfWeek.SUNDAY);
        int city = provider.findWeight(null, BusinessType.CAFE_BEVERAGE, DayOfWeek.SUNDAY);

        assertEquals(-6, local);
        assertEquals(-5, city);
    }

    @Test
    @DisplayName("상권 밖 매장은 City weight를 받는다")
    void storeOutsideCommercialAreaUsesCity() {
        assertEquals(4, provider.findWeight(null, BusinessType.CHICKEN, DayOfWeek.FRIDAY));
        assertEquals(-3, provider.findWeight(null, BusinessType.CHICKEN, DayOfWeek.MONDAY));
    }
}
