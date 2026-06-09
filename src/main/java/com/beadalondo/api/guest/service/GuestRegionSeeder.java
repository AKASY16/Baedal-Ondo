package com.beadalondo.api.guest.service;

import com.beadalondo.api.guest.domain.GuestRegion;
import com.beadalondo.api.guest.repository.GuestRegionRepository;
import com.beadalondo.api.location.AddressCoordinateResolver;
import com.beadalondo.api.location.client.JusoAddressSearchClient;
import com.beadalondo.api.location.dto.JusoAddressRequest;
import com.beadalondo.api.location.dto.WeatherGridResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class GuestRegionSeeder {

    private static final String SEOUL = "서울특별시";
    private static final List<String> SEOUL_DISTRICTS = List.of(
            "종로구",
            "중구",
            "용산구",
            "성동구",
            "광진구",
            "동대문구",
            "중랑구",
            "성북구",
            "강북구",
            "도봉구",
            "노원구",
            "은평구",
            "서대문구",
            "마포구",
            "양천구",
            "강서구",
            "구로구",
            "금천구",
            "영등포구",
            "동작구",
            "관악구",
            "서초구",
            "강남구",
            "송파구",
            "강동구"
    );

    private final GuestRegionRepository guestRegionRepository;
    private final JusoAddressSearchClient jusoAddressSearchClient;
    private final AddressCoordinateResolver addressCoordinateResolver;
    private final boolean enabled;

    public GuestRegionSeeder(GuestRegionRepository guestRegionRepository,
                             JusoAddressSearchClient jusoAddressSearchClient,
                             AddressCoordinateResolver addressCoordinateResolver,
                             @Value("${guest.region.seed.enabled:true}") boolean enabled) {
        this.guestRegionRepository = guestRegionRepository;
        this.jusoAddressSearchClient = jusoAddressSearchClient;
        this.addressCoordinateResolver = addressCoordinateResolver;
        this.enabled = enabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedSeoulDistrictOffices() {
        if (!enabled) {
            log.info("게스트 지역 초기 적재를 건너뜁니다.");
            return;
        }

        long activeSeoulRegionCount = guestRegionRepository.findBySidoNameAndActiveTrue(SEOUL).size();
        if (activeSeoulRegionCount >= SEOUL_DISTRICTS.size()) {
            log.info("서울 게스트 지역 데이터가 이미 적재되어 있습니다. count={}", activeSeoulRegionCount);
            return;
        }

        int savedCount = 0;
        for (String district : SEOUL_DISTRICTS) {
            if (guestRegionRepository.existsBySidoNameAndSigunguName(SEOUL, district)) {
                continue;
            }

            try {
                JusoAddressRequest address = jusoAddressSearchClient.searchFirst(SEOUL + " " + district + "청");
                WeatherGridResult grid = addressCoordinateResolver.addressCoordinateResolver(address);

                GuestRegion guestRegion = new GuestRegion(
                        address.getRoadFullAddr(),
                        address.getRoadAddrPart1(),
                        address.getJibunAddr(),
                        address.getAddrDetail(),
                        address.getZipNo(),
                        address.getSiNm(),
                        address.getSggNm(),
                        address.getEmdNm(),
                        address.getAdmCd(),
                        address.getRnMgtSn(),
                        address.getBdMgtSn(),
                        address.getRn(),
                        address.getUdrtYn(),
                        address.getBuldMnnm(),
                        address.getBuldSlno(),
                        grid.getNx(),
                        grid.getNy()
                );

                guestRegionRepository.save(guestRegion);
                savedCount++;
            } catch (RuntimeException e) {
                log.warn("서울 게스트 지역 데이터 적재 실패. district={}", district, e);
            }
        }

        log.info("서울 게스트 지역 데이터 적재 완료. savedCount={}", savedCount);
    }

    private static final Logger log = LoggerFactory.getLogger(GuestRegionSeeder.class);
}
