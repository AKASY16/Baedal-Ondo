package com.baedalondo.api.store.service;
import com.baedalondo.api.airquality.util.KoreanAddressParser;
import com.baedalondo.api.auth.service.CurrentUserService;
import com.baedalondo.api.commercialarea.dto.CommercialAreaMatch;
import com.baedalondo.api.commercialarea.locator.CommercialAreaLocator;
import com.baedalondo.api.location.AddressCoordinateResolver;
import com.baedalondo.api.location.JusoAddressVerifier;
import com.baedalondo.api.location.dto.JusoAddressRequest;
import com.baedalondo.api.location.dto.ResolvedCoordinateResult;
import com.baedalondo.api.location.dto.WeatherGridResult;
import com.baedalondo.api.store.dto.StoreEditRequest;
import com.baedalondo.api.store.factory.StoreFactory;
import com.baedalondo.api.store.domain.Store;
import com.baedalondo.api.store.dto.StoreRegisterRequest;
import com.baedalondo.api.store.repository.StoreRepository;
import com.baedalondo.api.user.domain.UserAccount;
import com.baedalondo.api.user.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
public class StoreService {

    private final StoreRepository storeRepository;
    private final AddressCoordinateResolver addressCoordinateResolver;
    private final CommercialAreaLocator commercialAreaLocator;
    private final StoreFactory storeFactory;
    private final CurrentUserService currentUserService;
    private final UserAccountRepository userAccountRepository;
    private final JusoAddressVerifier jusoAddressVerifier;

    public StoreService(StoreRepository storeRepository,
                        AddressCoordinateResolver addressCoordinateResolver,
                        CommercialAreaLocator commercialAreaLocator,
                        StoreFactory storeFactory,
                        CurrentUserService currentUserService,
                        UserAccountRepository userAccountRepository,
                        JusoAddressVerifier jusoAddressVerifier) {
        this.storeRepository = storeRepository;
        this.addressCoordinateResolver = addressCoordinateResolver;
        this.commercialAreaLocator = commercialAreaLocator;
        this.storeFactory = storeFactory;
        this.currentUserService = currentUserService;
        this.userAccountRepository = userAccountRepository;
        this.jusoAddressVerifier = jusoAddressVerifier;
    }

    public Store registerStore(StoreRegisterRequest request) {

        validateRegisterRequest(request);

        // 사용자가 보낸 주소를 행안부 API로 다시 검증하고,
        // 서버가 확인한 주소 정보를 반환받는다.
        JusoAddressRequest verifiedAddress =
                jusoAddressVerifier.storeRegisterCheckAddress(request);

        // 검증된 주소를 기준으로 좌표를 계산한다.
        // WGS84 좌표는 격자 계산과 상권 판별에만 쓰고 Store에 저장하지 않는다.
        ResolvedCoordinateResult resolvedCoordinate =
                addressCoordinateResolver.resolveCoordinate(verifiedAddress);

        WeatherGridResult weatherGridCoordinate =
                resolvedCoordinate.getWeatherGrid();

        CommercialAreaMatch commercialAreaMatch = commercialAreaLocator
                .find(
                        resolvedCoordinate.getLatitude(),
                        resolvedCoordinate.getLongitude()
                )
                .orElse(null);

        // Store에도 사용자가 보낸 원본 주소가 아니라
        // 검증된 주소를 저장한다.
        Store store = storeFactory.storeCreate(
                request,
                verifiedAddress,
                weatherGridCoordinate,
                commercialAreaMatch
        );

        // 인증된 세션의 ID이므로 존재가 보장된다.
        // FK 설정에만 쓰므로 프록시로 조회 쿼리를 생략한다.
        UserAccount user = userAccountRepository
                .getReferenceById(currentUserService.getCurrentUserId());

        store.setUser(user);

        return storeRepository.save(store);
    }

    @Transactional
    public Store editStore(Long storeId, StoreEditRequest request) {

        validateEditRequest(request);

        Long currentUserId = currentUserService.getCurrentUserId();

        Store currentStore = storeRepository
                .findByIdAndUserId(storeId, currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("가게를 찾을 수 없습니다."));

        JusoAddressRequest requestedAddress = request.getJusoAddress();

        boolean addressChanged =
                !Objects.equals(
                        requestedAddress.getRoadFullAddr(),
                        currentStore.getAddress()
                );

        boolean nameChanged =
                !Objects.equals(
                        request.getName(),
                        currentStore.getName()
                );

        boolean businessTypeChanged =
                request.getBusinessType() != currentStore.getBusinessType();

        boolean addressDetailChanged =
                !Objects.equals(
                        requestedAddress.getAddrDetail(),
                        currentStore.getAddressDetail()
                );

        if (!addressChanged
                && !nameChanged
                && !businessTypeChanged
                && !addressDetailChanged) {
            return currentStore;
        }

        currentStore.setName(request.getName());
        currentStore.setBusinessType(request.getBusinessType());

        if (addressChanged) {

            // 사용자가 보낸 주소를 행안부 API로 다시 검증하고,
            // 서버가 확인한 주소 정보를 사용한다.
            JusoAddressRequest verifiedAddress =
                    jusoAddressVerifier.storeEditCheckAddress(request);

            // 검증된 주소를 기준으로 기상청 격자 및 상권 좌표를 계산한다.
            ResolvedCoordinateResult resolvedCoordinate =
                    addressCoordinateResolver.resolveCoordinate(verifiedAddress);

            WeatherGridResult weatherGridCoordinate =
                    resolvedCoordinate.getWeatherGrid();

            CommercialAreaMatch commercialAreaMatch = commercialAreaLocator
                    .find(
                            resolvedCoordinate.getLatitude(),
                            resolvedCoordinate.getLongitude()
                    )
                    .orElse(null);

            // Store에도 클라이언트 원본 주소가 아니라 검증된 주소를 반영한다.
            storeFactory.editStore(
                    currentStore,
                    request,
                    verifiedAddress,
                    weatherGridCoordinate,
                    commercialAreaMatch
            );

        } else if (addressDetailChanged) {

            // 상세주소는 사용자가 직접 입력하는 값이므로 request 값을 사용한다.
            currentStore.setAddressDetail(
                    requestedAddress.getAddrDetail()
            );
        }

        return currentStore;
    }



    @Transactional(readOnly = true)
    public Store getCurrentUserStoreById(Long storeId) {

        Long currentUserId = currentUserService.getCurrentUserId();

        return storeRepository.findByIdAndUserId(storeId, currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("접근할 수 없는 가게입니다."));
    }


    @Transactional(readOnly = true)
    public List<Store> getCurrentLoginUserStores() {
        Long currentUserId = currentUserService.getCurrentUserId();
        return storeRepository.findByUserIdOrderByIdAsc(currentUserId);
    }


    private void validateRegisterRequest(StoreRegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("가게 등록 요청이 없습니다.");
        }

        if (isBlank(request.getName())) {
            throw new IllegalArgumentException("가게명이 없습니다.");
        }

        if (request.getBusinessType() == null) {
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

    private void validateEditRequest(StoreEditRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("가게 수정 요청이 없습니다.");
        }

        if (isBlank(request.getName())) {
            throw new IllegalArgumentException("가게명이 없습니다.");
        }

        if (request.getBusinessType() == null) {
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
