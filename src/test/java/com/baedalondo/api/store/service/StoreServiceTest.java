package com.baedalondo.api.store.service;

import com.baedalondo.api.airquality.util.KoreanAddressParser;
import com.baedalondo.api.auth.service.CurrentUserService;
import com.baedalondo.api.commercialarea.dto.CommercialAreaMatch;
import com.baedalondo.api.commercialarea.locator.CommercialAreaLocator;
import com.baedalondo.api.location.AddressCoordinateResolver;
import com.baedalondo.api.location.dto.JusoAddressRequest;
import com.baedalondo.api.location.dto.ResolvedCoordinateResult;
import com.baedalondo.api.location.dto.WeatherGridResult;
import com.baedalondo.api.location.dto.Wgs84CoordinateResult;
import com.baedalondo.api.store.domain.BusinessType;
import com.baedalondo.api.store.domain.Store;
import com.baedalondo.api.store.dto.StoreRegisterRequest;
import com.baedalondo.api.store.factory.StoreFactory;
import com.baedalondo.api.store.repository.StoreRepository;
import com.baedalondo.api.user.domain.UserAccount;
import com.baedalondo.api.user.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    private static final double LATITUDE = 37.5665;
    private static final double LONGITUDE = 126.9780;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private AddressCoordinateResolver addressCoordinateResolver;

    @Mock
    private CommercialAreaLocator commercialAreaLocator;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private UserAccountRepository userAccountRepository;

    private StoreService storeService;

    @BeforeEach
    void setUp() {
        // StoreFactory는 순수 매핑 로직이라 실제 구현을 그대로 쓴다.
        StoreFactory storeFactory = new StoreFactory(new KoreanAddressParser());

        storeService = new StoreService(
                storeRepository,
                addressCoordinateResolver,
                commercialAreaLocator,
                storeFactory,
                currentUserService,
                userAccountRepository
        );

        // 업종 검증에서 먼저 실패하는 테스트도 있어 lenient로 둔다.
        lenient().when(addressCoordinateResolver.resolveCoordinate(any()))
                .thenReturn(new ResolvedCoordinateResult(
                        new WeatherGridResult(60, 127),
                        new Wgs84CoordinateResult(LONGITUDE, LATITUDE)));

        lenient().when(currentUserService.getCurrentUserId()).thenReturn(1L);
        lenient().when(userAccountRepository.getReferenceById(1L))
                .thenReturn(mock(UserAccount.class));
        lenient().when(storeRepository.save(any(Store.class)))
                .thenAnswer(it -> it.getArgument(0));
    }

    @Test
    @DisplayName("상권 안 매장은 nx, ny와 상권정보가 함께 저장된다")
    void savesCommercialAreaWithWeatherGrid() {

        when(commercialAreaLocator.find(LATITUDE, LONGITUDE))
                .thenReturn(Optional.of(new CommercialAreaMatch(
                        "3110008", "배화여자대학교(박노수미술관)", "A", "골목상권")));

        storeService.registerStore(registerRequest());

        Store saved = capturedStore();

        assertEquals(BusinessType.CHICKEN, saved.getBusinessType());
        assertEquals(60, saved.getNx().intValue());
        assertEquals(127, saved.getNy().intValue());
        assertEquals("3110008", saved.getCommercialAreaCode());
        assertEquals("배화여자대학교(박노수미술관)", saved.getCommercialAreaName());
        assertEquals("A", saved.getCommercialAreaTypeCode());
        assertEquals("골목상권", saved.getCommercialAreaTypeName());
    }

    @Test
    @DisplayName("상권을 찾지 못해도 매장 등록은 성공하고 상권코드는 null로 저장된다")
    void savesStoreWithoutCommercialArea() {

        when(commercialAreaLocator.find(anyDouble(), anyDouble()))
                .thenReturn(Optional.empty());

        storeService.registerStore(registerRequest());

        Store saved = capturedStore();

        assertEquals(60, saved.getNx().intValue());
        assertEquals(127, saved.getNy().intValue());
        assertNull(saved.getCommercialAreaCode());
        assertNull(saved.getCommercialAreaName());
        assertNull(saved.getCommercialAreaTypeCode());
        assertNull(saved.getCommercialAreaTypeName());
        assertEquals("비댈온도식당", saved.getName());
    }

    @Test
    @DisplayName("업종을 선택하지 않으면 매장 등록에 실패한다")
    void rejectsMissingBusinessType() {

        StoreRegisterRequest request = registerRequest(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> storeService.registerStore(request));

        assertEquals("업종이 없습니다.", exception.getMessage());
        verify(storeRepository, never()).save(any(Store.class));
    }

    @Test
    @DisplayName("상권 판별에는 저장하지 않는 WGS84 좌표를 위도, 경도 순서로 넘긴다")
    void passesLatitudeAndLongitudeInOrder() {

        when(commercialAreaLocator.find(anyDouble(), anyDouble()))
                .thenReturn(Optional.empty());

        storeService.registerStore(registerRequest());

        verify(commercialAreaLocator).find(LATITUDE, LONGITUDE);
    }

    private Store capturedStore() {
        ArgumentCaptor<Store> captor = ArgumentCaptor.forClass(Store.class);
        verify(storeRepository).save(captor.capture());
        return captor.getValue();
    }

    private StoreRegisterRequest registerRequest() {
        return registerRequest(BusinessType.CHICKEN);
    }

    private StoreRegisterRequest registerRequest(BusinessType businessType) {

        JusoAddressRequest juso = new JusoAddressRequest();
        juso.setRoadFullAddr("서울특별시 종로구 필운대로 1");
        juso.setRoadAddrPart1("서울특별시 종로구 필운대로 1");
        juso.setJibunAddr("서울특별시 종로구 필운동 1");
        juso.setZipNo("03042");
        juso.setSiNm("서울특별시");
        juso.setSggNm("종로구");
        juso.setEmdNm("필운동");
        juso.setAdmCd("1111017400");
        juso.setRnMgtSn("111103100014");
        juso.setBdMgtSn("1111017400100010000000001");
        juso.setRn("필운대로");
        juso.setUdrtYn("0");
        juso.setBuldMnnm("1");
        juso.setBuldSlno("0");

        StoreRegisterRequest request = new StoreRegisterRequest();
        request.setName("비댈온도식당");
        request.setBusinessType(businessType);
        request.setJusoAddress(juso);

        return request;
    }
}
