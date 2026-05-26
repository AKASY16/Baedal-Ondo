package com.beadalondo.api.store.service;
import com.beadalondo.api.store.domain.Store;
import com.beadalondo.api.store.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
public class StoreService {

    private final StoreRepository storeRepository;

    public StoreService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

//    public Long registerStore() {
//
//    }

    @Transactional(readOnly = true)
    public Store getCurrentStore() {
        // TODO: 추후, 로그인 한 사람의 가게 정보를 반환해주는 메서드로 변경
        //  현재는 테스트용으로 DB에 등록된 가게 중 랜덤 1개 반환
        List<Store> stores = storeRepository.findAll();

        if (stores.isEmpty()) {
            throw new IllegalStateException("등록된 가게가 없습니다.");
        }

        int randomIndex = ThreadLocalRandom.current().nextInt(stores.size());

        return stores.get(randomIndex);
    }

}