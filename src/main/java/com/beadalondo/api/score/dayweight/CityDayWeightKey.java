package com.beadalondo.api.score.dayweight;

import com.beadalondo.api.store.domain.BusinessType;

import java.time.DayOfWeek;

/**
 서울 전체 fallback DayWeight 조회 키.
 **/
public record CityDayWeightKey(BusinessType businessType,
                               DayOfWeek dayOfWeek) {
}
