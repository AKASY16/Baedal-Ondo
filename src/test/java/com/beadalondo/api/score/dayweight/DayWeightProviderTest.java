package com.beadalondo.api.score.dayweight;

import com.beadalondo.api.store.domain.BusinessType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 Local -> City -> 0 fallback 규칙 검증.
 실제 CSV가 아니라 의도가 드러나는 fixture로 확인한다.
 **/
class DayWeightProviderTest {

    private static final String AREA_WITH_LOCAL = "3120029";
    private static final String AREA_WITHOUT_LOCAL = "9999999";

    // 같은 상권에 KOREAN_FOOD Local만 있고 CHICKEN Local은 없는 상황
    private final DayWeightProvider provider = new DayWeightProvider(
            Map.of(
                    new LocalDayWeightKey(AREA_WITH_LOCAL, BusinessType.KOREAN_FOOD, DayOfWeek.MONDAY), 5,
                    new LocalDayWeightKey(AREA_WITH_LOCAL, BusinessType.KOREAN_FOOD, DayOfWeek.SUNDAY), -6
            ),
            Map.of(
                    new CityDayWeightKey(BusinessType.KOREAN_FOOD, DayOfWeek.MONDAY), 0,
                    new CityDayWeightKey(BusinessType.KOREAN_FOOD, DayOfWeek.SUNDAY), -6,
                    new CityDayWeightKey(BusinessType.CHICKEN, DayOfWeek.MONDAY), -3,
                    new CityDayWeightKey(BusinessType.CHICKEN, DayOfWeek.FRIDAY), 4
            )
    );

    @Test
    @DisplayName("Local 데이터가 있으면 Local weight를 반환한다")
    void returnsLocalWeight() {
        assertEquals(5, provider.findWeight(
                AREA_WITH_LOCAL, BusinessType.KOREAN_FOOD, DayOfWeek.MONDAY));
    }

    @Test
    @DisplayName("Local과 City 값이 다르면 Local이 우선한다")
    void localTakesPrecedenceOverCity() {
        // City의 KOREAN_FOOD MONDAY는 0이지만 Local이 5이므로 5가 나와야 한다.
        assertEquals(5, provider.findWeight(
                AREA_WITH_LOCAL, BusinessType.KOREAN_FOOD, DayOfWeek.MONDAY));
    }

    @Test
    @DisplayName("상권은 있지만 해당 업종 Local이 없으면 City로 fallback한다")
    void fallsBackToCityWhenBusinessTypeMissing() {
        // 같은 상권에 KOREAN_FOOD Local은 있지만 CHICKEN Local은 없다.
        assertEquals(-3, provider.findWeight(
                AREA_WITH_LOCAL, BusinessType.CHICKEN, DayOfWeek.MONDAY));
    }

    @Test
    @DisplayName("같은 상권 같은 업종이라도 요일 데이터가 없으면 City로 fallback한다")
    void fallsBackToCityWhenDayMissing() {
        assertEquals(-6, provider.findWeight(
                AREA_WITH_LOCAL, BusinessType.KOREAN_FOOD, DayOfWeek.SUNDAY));
        assertEquals(4, provider.findWeight(
                AREA_WITH_LOCAL, BusinessType.CHICKEN, DayOfWeek.FRIDAY));
    }

    @Test
    @DisplayName("commercialAreaCode가 null이면 City로 fallback한다")
    void fallsBackToCityWhenAreaCodeIsNull() {
        assertEquals(-3, provider.findWeight(
                null, BusinessType.CHICKEN, DayOfWeek.MONDAY));
    }

    @Test
    @DisplayName("존재하지 않는 commercialAreaCode면 City로 fallback한다")
    void fallsBackToCityWhenAreaCodeIsUnknown() {
        assertEquals(-3, provider.findWeight(
                AREA_WITHOUT_LOCAL, BusinessType.CHICKEN, DayOfWeek.MONDAY));
    }

    @Test
    @DisplayName("City 값을 정상 조회한다")
    void returnsCityWeight() {
        assertEquals(4, provider.findWeight(
                null, BusinessType.CHICKEN, DayOfWeek.FRIDAY));
    }

    @Test
    @DisplayName("Local과 City 모두 없으면 0을 반환한다")
    void returnsZeroWhenNothingFound() {
        assertEquals(0, provider.findWeight(
                AREA_WITH_LOCAL, BusinessType.BAKERY, DayOfWeek.MONDAY));
    }

    @Test
    @DisplayName("businessType이나 dayOfWeek가 null이면 0을 반환한다")
    void returnsZeroWhenKeyPartIsNull() {
        assertEquals(0, provider.findWeight(AREA_WITH_LOCAL, null, DayOfWeek.MONDAY));
        assertEquals(0, provider.findWeight(AREA_WITH_LOCAL, BusinessType.KOREAN_FOOD, null));
    }

    @Test
    @DisplayName("빈 문자열 commercialAreaCode도 City로 fallback한다")
    void treatsBlankAreaCodeAsMissing() {
        assertEquals(-3, provider.findWeight(
                "  ", BusinessType.CHICKEN, DayOfWeek.MONDAY));
    }
}
