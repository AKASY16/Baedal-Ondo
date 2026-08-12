package com.baedalondo.api.score.timeweight;

import com.baedalondo.api.store.domain.BusinessType;

public record LocalTimeWeightKey(String commercialAreaCode,
                                 BusinessType businessType,
                                 TimeBand timeBand) {
}
