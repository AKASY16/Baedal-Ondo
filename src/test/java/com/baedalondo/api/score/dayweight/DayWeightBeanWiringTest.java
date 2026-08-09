package com.baedalondo.api.score.dayweight;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 DayWeightProvider는 생성자가 2개(Spring 주입용, 테스트 fixture용)라
 Spring이 올바른 생성자를 골라 시작 시 CSV를 로딩하는지 확인한다.
 DB가 필요 없도록 두 빈만 올리는 최소 컨텍스트를 쓴다.
 **/
@SpringJUnitConfig(classes = {DayWeightCsvLoader.class, DayWeightProvider.class})
class DayWeightBeanWiringTest {

    @Autowired
    private DayWeightProvider provider;

    @Test
    @DisplayName("빈 생성 시점에 CSV가 로딩된다")
    void loadsCsvOnBeanCreation() {
        assertEquals(32_403, provider.localKeyCount());
        assertEquals(63, provider.cityKeyCount());
    }
}
