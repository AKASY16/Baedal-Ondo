package com.baedalondo.api.score.timeweight;

import com.baedalondo.api.store.domain.BusinessType;

public record CityTimeWeightKey(BusinessType businessType,
                                TimeBand timeBand) {
}
