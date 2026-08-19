package com.baedalondo.api.store.repository;

import com.baedalondo.api.store.domain.Store;
import com.baedalondo.api.weather.domain.WeatherGrid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByIdAndUserId(Long storeId, Long userId);

    List<Store> findByUserIdOrderByIdAsc(Long userId);

    /**
     사전 적재 대상 격자. 날씨 캐시가 격자 단위라 매장 수가 아니라 격자 수만큼만 호출한다.
     좌표가 없는 매장은 조회해도 호출할 수 없으므로 제외한다.
     */
    @Query("select distinct new com.baedalondo.api.weather.domain.WeatherGrid(s.nx, s.ny) "
            + "from Store s where s.nx is not null and s.ny is not null")
    List<WeatherGrid> findDistinctWeatherGrids();

    /** 대기질은 시도 단위로 조회하므로 시도명만 모은다. 정규화는 호출한 쪽에서 한다. */
    @Query("select distinct s.sidoName from Store s where s.sidoName is not null")
    List<String> findDistinctSidoNames();

}