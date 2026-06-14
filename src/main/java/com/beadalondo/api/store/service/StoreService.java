package com.beadalondo.api.store.service;
import com.beadalondo.api.airquality.util.KoreanAddressParser;
import com.beadalondo.api.auth.service.CurrentUserService;
import com.beadalondo.api.location.AddressCoordinateResolver;
import com.beadalondo.api.location.dto.JusoAddressRequest;
import com.beadalondo.api.location.dto.WeatherGridResult;
import com.beadalondo.api.store.factory.StoreFactory;
import com.beadalondo.api.store.domain.Store;
import com.beadalondo.api.store.dto.StoreRegisterRequest;
import com.beadalondo.api.store.repository.StoreRepository;
import com.beadalondo.api.user.domain.UserAccount;
import com.beadalondo.api.user.repository.UserAccountRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
public class StoreService {

    private final StoreRepository storeRepository;
    private final AddressCoordinateResolver addressCoordinateResolver;
    private final StoreFactory storeFactory;
    private final CurrentUserService currentUserService;
    private final UserAccountRepository userAccountRepository;

    public StoreService(StoreRepository storeRepository,
                        AddressCoordinateResolver addressCoordinateResolver,
                        StoreFactory storeFactory,
                        CurrentUserService currentUserService,
                        UserAccountRepository userAccountRepository) {
        this.storeRepository = storeRepository;
        this.addressCoordinateResolver = addressCoordinateResolver;
        this.storeFactory = storeFactory;
        this.currentUserService = currentUserService;
        this.userAccountRepository = userAccountRepository;
    }

    public Store registerStore(StoreRegisterRequest request) {

        validateRegisterRequest(request);

        JusoAddressRequest jusoAddress =  request.getJusoAddress();

        WeatherGridResult weatherGridCoordinate =
                addressCoordinateResolver.addressCoordinateResolver(jusoAddress);

        Store store = storeFactory.storeCreate(request, weatherGridCoordinate);

        Long currentUserAccountId = currentUserService.getCurrentUserAccountId();

        UserAccount owner = userAccountRepository.findById(currentUserAccountId)
                .orElseThrow(() -> new IllegalStateException("로그인 사용자를 찾을 수 없습니다."));

        store.setOwner(owner);

        return storeRepository.save(store);

    }

    @Transactional(readOnly = true)
    public Store getCurrentUserStoreById(Long storeId) {

        Long currentUserAccountId = currentUserService.getCurrentUserAccountId();

        return storeRepository.findByIdAndOwnerId(storeId, currentUserAccountId)
                .orElseThrow(() -> new IllegalArgumentException("접근할 수 없는 가게입니다."));
    }


    @Transactional(readOnly = true)
    public List<Store> getStores() {
        return storeRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Transactional(readOnly = true)
    public List<Store> getCurrentLoginUserStores() {
        Long currentUserAccountId = currentUserService.getCurrentUserAccountId();
        return storeRepository.findByOwnerIdOrderByIdAsc(currentUserAccountId);
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
