package com.beadalondo.api.guest.service;

import com.beadalondo.api.guest.domain.GuestRegion;
import com.beadalondo.api.guest.dto.GuestRegionRegisterRequest;
import com.beadalondo.api.guest.repository.GuestRegionRepository;
import com.beadalondo.api.location.AddressCoordinateResolver;
import com.beadalondo.api.location.dto.JusoAddressRequest;
import com.beadalondo.api.location.dto.WeatherGridResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
public class GuestRegionService {

    private final GuestRegionRepository guestRegionRepository;
    private final AddressCoordinateResolver addressCoordinateResolver;

    public GuestRegionService(GuestRegionRepository guestRegionRepository,
                              AddressCoordinateResolver addressCoordinateResolver) {
        this.guestRegionRepository = guestRegionRepository;
        this.addressCoordinateResolver = addressCoordinateResolver;
    }


    public GuestRegion getRandomSeoulRegion() {
        List<GuestRegion> regions =
                guestRegionRepository.findBySidoNameAndActiveTrue("서울특별시");

        if (regions.isEmpty()) {
            throw new IllegalStateException("등록된 서울 게스트 지역이 없습니다.");
        }

        int randomIndex = ThreadLocalRandom.current().nextInt(regions.size());

        return regions.get(randomIndex);
    }

    public GuestRegion getGuestRegion(Long regionId) {
        return guestRegionRepository.findById(regionId)
                .orElseThrow(() -> new IllegalArgumentException("게스트 지역을 찾을 수 없습니다."));
    }





    public GuestRegion registerGuestRegion(GuestRegionRegisterRequest request) {
        validateRegisterRequest(request);

        JusoAddressRequest jusoAddress = request.getJusoAddress();
        WeatherGridResult weatherGrid = addressCoordinateResolver.addressCoordinateResolver(jusoAddress);

        GuestRegion guestRegion = new GuestRegion(
                jusoAddress.getRoadFullAddr(),
                jusoAddress.getRoadAddrPart1(),
                jusoAddress.getJibunAddr(),
                jusoAddress.getAddrDetail(),
                jusoAddress.getZipNo(),
                jusoAddress.getSiNm(),
                jusoAddress.getSggNm(),
                jusoAddress.getEmdNm(),
                jusoAddress.getAdmCd(),
                jusoAddress.getRnMgtSn(),
                jusoAddress.getBdMgtSn(),
                jusoAddress.getRn(),
                jusoAddress.getUdrtYn(),
                jusoAddress.getBuldMnnm(),
                jusoAddress.getBuldSlno(),
                weatherGrid.getNx(),
                weatherGrid.getNy()
        );

        return guestRegionRepository.save(guestRegion);
    }

    private void validateRegisterRequest(GuestRegionRegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("GuestRegion registration request is required.");
        }

        if (request.getJusoAddress() == null) {
            throw new IllegalArgumentException("Juso address is required.");
        }

        JusoAddressRequest juso = request.getJusoAddress();

        if (isBlank(juso.getRoadFullAddr())) {
            throw new IllegalArgumentException("roadFullAddr is required.");
        }

        if (isBlank(juso.getSiNm())) {
            throw new IllegalArgumentException("siNm is required.");
        }

        if (isBlank(juso.getSggNm())) {
            throw new IllegalArgumentException("sggNm is required.");
        }

        if (isBlank(juso.getAdmCd())) {
            throw new IllegalArgumentException("admCd is required.");
        }

        if (isBlank(juso.getRnMgtSn())) {
            throw new IllegalArgumentException("rnMgtSn is required.");
        }

        if (isBlank(juso.getUdrtYn())) {
            throw new IllegalArgumentException("udrtYn is required.");
        }

        if (isBlank(juso.getBuldMnnm())) {
            throw new IllegalArgumentException("buldMnnm is required.");
        }

        if (isBlank(juso.getBuldSlno())) {
            throw new IllegalArgumentException("buldSlno is required.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
