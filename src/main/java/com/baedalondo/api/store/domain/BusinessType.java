package com.baedalondo.api.store.domain;

/**
 배달온도가 지원하는 업종.
 서울시 서비스업종코드(CS100001 등)는 외부 데이터 소스의 코드이므로
 여기에 두지 않고 오프라인 전처리 단계에서 따로 매핑한다.
 **/
public enum BusinessType {

    KOREAN_FOOD("한식"),
    CHINESE_FOOD("중식"),
    JAPANESE_FOOD("일식"),
    WESTERN_FOOD("양식"),
    CHICKEN("치킨"),
    FAST_FOOD("패스트푸드"),
    BUNSIK("분식"),
    CAFE_BEVERAGE("카페·음료"),
    BAKERY("제과·베이커리");

    private final String displayName;

    BusinessType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
