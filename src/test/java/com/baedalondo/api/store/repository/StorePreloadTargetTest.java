package com.baedalondo.api.store.repository;

import com.baedalondo.api.store.domain.BusinessType;
import com.baedalondo.api.store.domain.Store;
import com.baedalondo.api.weather.domain.WeatherGrid;
import com.baedalondo.api.support.MySqlTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 사전 적재 대상을 뽑는 조회를 확인한다.
 스케줄러가 매시 도는 만큼 호출 수가 격자 수와 시도 수를 넘지 않아야 한다.
 **/
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class StorePreloadTargetTest extends MySqlTestSupport {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private StoreRepository storeRepository;

    @Test
    @DisplayName("같은 격자의 매장이 여러 개여도 격자는 한 번만 나온다")
    void deduplicatesWeatherGrids() {
        entityManager.persist(store("서울", 60, 127));
        entityManager.persist(store("서울", 60, 127));
        entityManager.persist(store("서울", 61, 126));
        entityManager.flush();

        List<WeatherGrid> grids = storeRepository.findDistinctWeatherGrids();

        assertEquals(2, grids.size());
        assertTrue(grids.contains(new WeatherGrid(60, 127)));
        assertTrue(grids.contains(new WeatherGrid(61, 126)));
    }

    @Test
    @DisplayName("격자 좌표가 없는 매장은 대상에서 빠진다")
    void excludesStoresWithoutGrid() {
        // 좌표가 없으면 기상청을 호출할 수 없다. 조회해봐야 실패만 늘어난다.
        entityManager.persist(store("서울", null, null));
        entityManager.persist(store("서울", 60, 127));
        entityManager.flush();

        List<WeatherGrid> grids = storeRepository.findDistinctWeatherGrids();

        assertEquals(List.of(new WeatherGrid(60, 127)), grids);
    }

    @Test
    @DisplayName("시도명은 중복을 제거해 돌려준다")
    void deduplicatesSidoNames() {
        entityManager.persist(store("서울", 60, 127));
        entityManager.persist(store("서울", 61, 126));
        entityManager.persist(store("경기", 62, 125));
        entityManager.flush();

        List<String> sidoNames = storeRepository.findDistinctSidoNames();

        assertEquals(2, sidoNames.size());
        assertTrue(sidoNames.containsAll(List.of("서울", "경기")));
    }

    private Store store(String sidoName, Integer nx, Integer ny) {
        return new Store(
                "온도식당",
                BusinessType.CHICKEN,
                null, null, null, null, null,
                sidoName, null, null,
                null, null, null,
                null, null, null, null,
                nx, ny
        );
    }
}
