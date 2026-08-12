package com.baedalondo.api.score.timeweight;

import com.baedalondo.api.score.status.TimeDemandLevel;
import com.baedalondo.api.store.domain.BusinessType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.Map;

/** 상권 x 업종 x 시간대 등급을 Local -> City 순서로 조회한다. */
@Component
public class TimeWeightProvider {

    private static final Logger log = LoggerFactory.getLogger(TimeWeightProvider.class);

    private final Map<LocalTimeWeightKey, TimeDemandLevel> localLevels;
    private final Map<CityTimeWeightKey, TimeDemandLevel> cityLevels;

    @Autowired
    public TimeWeightProvider(TimeWeightCsvLoader loader) {
        this(loader.loadLocal(), loader.loadCity());
        log.info("TimeWeight 로딩 완료. local={}건, city={}건",
                localLevels.size(), cityLevels.size());
    }

    TimeWeightProvider(Map<LocalTimeWeightKey, TimeDemandLevel> localLevels,
                       Map<CityTimeWeightKey, TimeDemandLevel> cityLevels) {
        this.localLevels = Map.copyOf(localLevels);
        this.cityLevels = Map.copyOf(cityLevels);
    }

    /** 데이터가 없거나 업종이 없는 요청이면 null을 반환해 기존 공통 시간표로 fallback한다. */
    public TimeDemandLevel findDemandLevel(String commercialAreaCode,
                                           BusinessType businessType,
                                           LocalTime time) {
        if (businessType == null || time == null) {
            return null;
        }

        TimeBand timeBand = TimeBand.from(time);
        if (commercialAreaCode != null && !commercialAreaCode.isBlank()) {
            TimeDemandLevel local = localLevels.get(
                    new LocalTimeWeightKey(commercialAreaCode, businessType, timeBand));
            if (local != null) {
                return local;
            }
        }

        TimeDemandLevel city = cityLevels.get(new CityTimeWeightKey(businessType, timeBand));
        if (city != null) {
            return city;
        }

        log.warn("TimeWeight를 찾지 못했습니다. commercialAreaCode={}, businessType={}, timeBand={}",
                commercialAreaCode, businessType, timeBand);
        return null;
    }

    public int localKeyCount() {
        return localLevels.size();
    }

    public int cityKeyCount() {
        return cityLevels.size();
    }
}
