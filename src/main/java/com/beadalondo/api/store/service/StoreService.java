package com.beadalondo.api.store.service;
import com.beadalondo.api.airquality.util.KoreanAddressParser;
import com.beadalondo.api.location.calculator.Epsg5179ToWgs84Converter;
import com.beadalondo.api.location.calculator.Wgs84ToWeatherGridConverter;
import com.beadalondo.api.location.client.JusoCoordinateClient;
import com.beadalondo.api.location.dto.JusoAddressRequest;
import com.beadalondo.api.location.dto.EntCoordinateResult;
import com.beadalondo.api.location.dto.WeatherGridResult;
import com.beadalondo.api.location.dto.Wgs84CoordinateResult;
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
    private final JusoCoordinateClient jusoCoordinateClient;
    private final Epsg5179ToWgs84Converter epsg5179ToWgs84Converter;
    private final Wgs84ToWeatherGridConverter wgs84ToWeatherGridConverter;
    private final KoreanAddressParser koreanAddressParser;

    public StoreService(StoreRepository storeRepository,
                        JusoCoordinateClient jusoCoordinateClient,
                        Epsg5179ToWgs84Converter epsg5179ToWgs84Converter,
                        Wgs84ToWeatherGridConverter wgs84ToWeatherGridConverter,
                        KoreanAddressParser koreanAddressParser) {
        this.storeRepository = storeRepository;
        this.jusoCoordinateClient = jusoCoordinateClient;
        this.epsg5179ToWgs84Converter = epsg5179ToWgs84Converter;
        this.wgs84ToWeatherGridConverter = wgs84ToWeatherGridConverter;
        this.koreanAddressParser = koreanAddressParser;
    }

    public Store registerStore(StoreRegisterRequest request) {

        validateRegisterRequest(request);

        JusoAddressRequest jusoAddress =  request.getJusoAddress();

        EntCoordinateResult entCoordinate =
                jusoCoordinateClient.getCoordinate(jusoAddress);

        Wgs84CoordinateResult wgsCoordinate =
                epsg5179ToWgs84Converter.epsg5179ToWgs84Converter(entCoordinate.getEntX(), entCoordinate.getEntY());

        WeatherGridResult weatherGridCoordinate =
                wgs84ToWeatherGridConverter.wgs84ToWeatherGridConverter(wgsCoordinate.getWgsX(), wgsCoordinate.getWgsY());

        Store store = new Store(
                request.getName(),
                request.getBusinessType(),

                jusoAddress.getRoadFullAddr(),   // address: 대표 표시 주소
                jusoAddress.getRoadAddrPart1(),  // roadAddress: 도로명주소
                jusoAddress.getJibunAddr(),      // jibunAddress: 지번주소
                jusoAddress.getAddrDetail(),
                jusoAddress.getZipNo(),

                koreanAddressParser.extractSidoName(jusoAddress.getSiNm()),
                jusoAddress.getSggNm(),
                jusoAddress.getEmdNm(),

                jusoAddress.getAdmCd(),
                jusoAddress.getRnMgtSn(),
                jusoAddress.getBdMgtSn(),

                jusoAddress.getRn(),
                jusoAddress.getUdrtYn(),
                jusoAddress.getBuldMnnm(),
                jusoAddress.getBuldSlno(),

                weatherGridCoordinate.getNx(),
                weatherGridCoordinate.getNy()
        );

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