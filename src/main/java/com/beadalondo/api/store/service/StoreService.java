package com.beadalondo.api.store.service;

import ch.qos.logback.core.rolling.helper.IntegerTokenConverter;
import com.beadalondo.api.store.domain.Store;
import com.beadalondo.api.store.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StoreService {

    private final StoreRepository storeRepository;

    public StoreService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    public Long registerStore(String name,
                              String businessType,
                              String address,
                              Double latitude,
                              Double longitude,
                              Integer nx,
                              Integer ny,
                              String district,
                              String dongCode) {

        Store store = new Store(
                name, //이름
                businessType, //업종
                address, //주소
                latitude, //위도
                longitude, //경도
                nx, // 기상청 API 격자 X좌표
                ny, // 기상청 API 격자 Y좌표
                district, //자치구
                dongCode //행정동
        );

        Store savedStore = storeRepository.save(store);

        return savedStore.getId();
    }

    @Transactional(readOnly = true)
    public Store getCurrentStore(){
        //TODO: 추후, 로그인 한 사람의 가게 정보를 반환해주는 메서드로 변경

        return storeRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("등록된 가게가 없습니다."));
    }

}