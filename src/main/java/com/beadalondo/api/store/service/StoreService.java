package com.beadalondo.api.store.service;
import com.beadalondo.api.airquality.util.KoreanAddressParser;
import com.beadalondo.api.location.AddressCoordinateResolver;
import com.beadalondo.api.location.dto.JusoAddressRequest;
import com.beadalondo.api.location.dto.WeatherGridResult;
import com.beadalondo.api.store.factory.StoreFactory;
import com.beadalondo.api.store.domain.Store;
import com.beadalondo.api.store.dto.StoreRegisterRequest;
import com.beadalondo.api.store.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
public class StoreService {

    private final StoreRepository storeRepository;
    private final AddressCoordinateResolver addressCoordinateResolver;
    private final StoreFactory storeFactory;

    public StoreService(StoreRepository storeRepository,
                        KoreanAddressParser koreanAddressParser,
                        AddressCoordinateResolver addressCoordinateResolver, StoreFactory storeFactory) {
        this.storeRepository = storeRepository;
        this.addressCoordinateResolver = addressCoordinateResolver;
        this.storeFactory = storeFactory;
    }

    public Store registerStore(StoreRegisterRequest request) {

        validateRegisterRequest(request);

        JusoAddressRequest jusoAddress =  request.getJusoAddress();

        WeatherGridResult weatherGridCoordinate =
                addressCoordinateResolver.addressCoordinateResolver(jusoAddress);

        Store store = storeFactory.storeCreate(request, weatherGridCoordinate);

        return storeRepository.save(store);

    }

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


    private void validateRegisterRequest(StoreRegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("가게 등록 요청이 없습니다.");
        }

        if (isBlank(request.getName())) {
            throw new IllegalArgumentException("가게명이 없습니다.");
        }

        if (isBlank(request.getBusinessType())) {
            throw new IllegalArgumentException("업종이 없습니다.");
        }

        if (request.getJusoAddress() == null) {
            throw new IllegalArgumentException("주소 정보가 없습니다.");
        }

        JusoAddressRequest juso = request.getJusoAddress();

        if (isBlank(juso.getAdmCd())) {
            throw new IllegalArgumentException("행정구역코드가 없습니다.");
        }

        if (isBlank(juso.getRnMgtSn())) {
            throw new IllegalArgumentException("도로명코드가 없습니다.");
        }

        if (isBlank(juso.getUdrtYn())) {
            throw new IllegalArgumentException("지하여부가 없습니다.");
        }

        if (isBlank(juso.getBuldMnnm())) {
            throw new IllegalArgumentException("건물본번이 없습니다.");
        }

        if (isBlank(juso.getBuldSlno())) {
            throw new IllegalArgumentException("건물부번이 없습니다.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

}