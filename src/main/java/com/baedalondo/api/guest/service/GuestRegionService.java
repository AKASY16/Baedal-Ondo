package com.baedalondo.api.guest.service;

import com.baedalondo.api.guest.domain.GuestRegion;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/** DB를 사용하지 않고 고정 CSV 지역을 제공한다. */
@Service
public class GuestRegionService {

    private final List<GuestRegion> regions;
    private final Map<Long, GuestRegion> regionById;

    public GuestRegionService(GuestRegionCsvLoader loader) {
        this.regions = loader.load();
        this.regionById = regions.stream()
                .collect(Collectors.toUnmodifiableMap(GuestRegion::getId, Function.identity()));
    }

    public GuestRegion getRandomSeoulRegion() {
        int randomIndex = ThreadLocalRandom.current().nextInt(regions.size());
        return regions.get(randomIndex);
    }

    public GuestRegion getGuestRegion(Long regionId) {
        if (regionId == null) {
            throw new IllegalArgumentException("게스트 지역 ID가 없습니다.");
        }

        GuestRegion region = regionById.get(regionId);
        if (region == null) {
            throw new IllegalArgumentException("게스트 지역을 찾을 수 없습니다. id=" + regionId);
        }
        return region;
    }

    public List<GuestRegion> getRegions() {
        return regions;
    }
}
