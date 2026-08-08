package com.beadalondo.api.store.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BusinessTypeTest {

    @Test
    @DisplayName("MVP가 지원하는 업종은 9개이며 OTHER 같은 기타 업종은 없다")
    void supportsExactlyNineTypes() {
        assertEquals(9, BusinessType.values().length);
    }

    @Test
    @DisplayName("화면에는 Enum 상수명이 아니라 한글 업종명이 노출된다")
    void exposesKoreanDisplayName() {
        assertEquals("한식", BusinessType.KOREAN_FOOD.getDisplayName());
        assertEquals("중식", BusinessType.CHINESE_FOOD.getDisplayName());
        assertEquals("일식", BusinessType.JAPANESE_FOOD.getDisplayName());
        assertEquals("양식", BusinessType.WESTERN_FOOD.getDisplayName());
        assertEquals("치킨", BusinessType.CHICKEN.getDisplayName());
        assertEquals("패스트푸드", BusinessType.FAST_FOOD.getDisplayName());
        assertEquals("분식", BusinessType.BUNSIK.getDisplayName());
        assertEquals("카페·음료", BusinessType.CAFE_BEVERAGE.getDisplayName());
        assertEquals("제과·베이커리", BusinessType.BAKERY.getDisplayName());
    }
}
