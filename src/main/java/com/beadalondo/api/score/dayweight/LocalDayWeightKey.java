package com.beadalondo.api.score.dayweight;

import com.beadalondo.api.store.domain.BusinessType;

import java.time.DayOfWeek;

/**
 상권별 DayWeight 조회 키.
 상권에 다른 업종 데이터가 있다는 이유로 Local을 쓰면 안 되므로
 상권코드와 업종을 반드시 함께 키로 사용한다.
 **/
public record LocalDayWeightKey(String commercialAreaCode,
                                BusinessType businessType,
                                DayOfWeek dayOfWeek) {
}
