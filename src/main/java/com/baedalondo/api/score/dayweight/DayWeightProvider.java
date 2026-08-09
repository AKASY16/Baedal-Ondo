package com.baedalondo.api.score.dayweight;

import com.baedalondo.api.store.domain.BusinessType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.Map;

/**
 상권 x 업종 x 요일 DayWeight를 조회한다.

 CSV는 애플리케이션 시작 시 한 번만 읽어 메모리 Map으로 들고 있는다.
 조회 순서는 Local -> City -> 0 이다.
 **/
@Component
public class DayWeightProvider {

    private static final Logger log = LoggerFactory.getLogger(DayWeightProvider.class);

    static final int NO_WEIGHT = 0;

    private final Map<LocalDayWeightKey, Integer> localWeights;
    private final Map<CityDayWeightKey, Integer> cityWeights;

    @Autowired
    public DayWeightProvider(DayWeightCsvLoader loader) {
        this(loader.loadLocal(), loader.loadCity());
        log.info("DayWeight 로딩 완료. local={}건, city={}건",
                localWeights.size(), cityWeights.size());
    }

    public DayWeightProvider(Map<LocalDayWeightKey, Integer> localWeights,
                             Map<CityDayWeightKey, Integer> cityWeights) {
        this.localWeights = Map.copyOf(localWeights);
        this.cityWeights = Map.copyOf(cityWeights);
    }

    /**
     상권 x 업종 Local 값이 있으면 그것을, 없으면 서울 전체 City 값을 쓴다.
     City마저 없는 비정상 상황에서는 요일 보정을 하지 않는다는 뜻으로 0을 준다.

     commercialAreaCode는 상권 밖 매장이면 null일 수 있다.
     */
    public int findWeight(String commercialAreaCode,
                          BusinessType businessType,
                          DayOfWeek dayOfWeek) {

        if (businessType == null || dayOfWeek == null) {
            return NO_WEIGHT;
        }

        if (commercialAreaCode != null && !commercialAreaCode.isBlank()) {
            Integer localWeight = localWeights.get(
                    new LocalDayWeightKey(commercialAreaCode, businessType, dayOfWeek));

            if (localWeight != null) {
                return localWeight;
            }
        }

        Integer cityWeight = cityWeights.get(new CityDayWeightKey(businessType, dayOfWeek));

        if (cityWeight != null) {
            return cityWeight;
        }

        log.warn("DayWeight를 찾지 못했습니다. commercialAreaCode={}, businessType={}, dayOfWeek={}",
                commercialAreaCode, businessType, dayOfWeek);

        return NO_WEIGHT;
    }

    public int localKeyCount() {
        return localWeights.size();
    }

    public int cityKeyCount() {
        return cityWeights.size();
    }
}
