package com.baedalondo.api.store.factory;

import com.baedalondo.api.airquality.util.KoreanAddressParser;
import com.baedalondo.api.commercialarea.dto.CommercialAreaMatch;
import com.baedalondo.api.location.dto.JusoAddressRequest;
import com.baedalondo.api.location.dto.WeatherGridResult;
import com.baedalondo.api.store.domain.Store;
import com.baedalondo.api.store.dto.StoreRegisterRequest;
import org.springframework.stereotype.Component;

@Component
public class StoreFactory {

    private final KoreanAddressParser koreanAddressParser;

    public  StoreFactory(KoreanAddressParser koreanAddressParser) {
        this.koreanAddressParser = koreanAddressParser;
    }

    public Store storeCreate(StoreRegisterRequest request,
                              WeatherGridResult weatherGridCoordinate){

        return storeCreate(request, weatherGridCoordinate, null);
    }

    /**
     * commercialAreaMatch는 상권을 찾지 못한 경우 null이며,
     * 이때 매장은 상권 정보 없이 정상 등록된다.
     */
    public Store storeCreate(StoreRegisterRequest request,
                              WeatherGridResult weatherGridCoordinate,
                              CommercialAreaMatch commercialAreaMatch){

        JusoAddressRequest jusoAddress =  request.getJusoAddress();

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

        store.assignCommercialArea(commercialAreaMatch);

        return store;
    }

}
