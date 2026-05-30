package com.beadalondo.api.store.factory;

import com.beadalondo.api.airquality.util.KoreanAddressParser;
import com.beadalondo.api.location.dto.JusoAddressRequest;
import com.beadalondo.api.location.dto.WeatherGridResult;
import com.beadalondo.api.store.domain.Store;
import com.beadalondo.api.store.dto.StoreRegisterRequest;
import org.springframework.stereotype.Component;

@Component
public class StoreFactory {

    private final KoreanAddressParser koreanAddressParser;

    public  StoreFactory(KoreanAddressParser koreanAddressParser) {
        this.koreanAddressParser = koreanAddressParser;
    }

    public Store storeCreate(StoreRegisterRequest request,
                              WeatherGridResult weatherGridCoordinate){

        JusoAddressRequest jusoAddress =  request.getJusoAddress();

        return new Store(
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
    }

}
