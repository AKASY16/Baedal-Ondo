package com.baedalondo.api;

import com.baedalondo.api.guest.domain.GuestRegion;
import com.baedalondo.api.guest.service.GuestRegionCsvLoader;
import com.baedalondo.api.guest.service.GuestRegionService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuestRegionCsvLoaderTest {

    private final GuestRegionCsvLoader loader = new GuestRegionCsvLoader();

    @Test
    void loadsAllSeoulDistrictOfficesFromClasspathCsv() {
        List<GuestRegion> regions = loader.load();

        assertEquals(25, regions.size());
        assertEquals(25, regions.stream().map(GuestRegion::getSigunguName).distinct().count());
        assertTrue(regions.stream().allMatch(region -> "서울특별시".equals(region.getSidoName())));
        assertTrue(regions.stream().allMatch(region -> region.getNx() != null && region.getNy() != null));
    }

    @Test
    void preservesQuotedCommaAndLeadingZeroPostalCode() {
        GuestRegionService service = new GuestRegionService(loader);

        GuestRegion seocho = service.getGuestRegion(15L);

        assertEquals("서초구청", seocho.getDisplayName());
        assertEquals("06750", seocho.getPostalCode());
        assertEquals(
                "서울특별시 서초구 서초동 1376-3 서초구청, 서초구의회",
                seocho.getJibunAddress()
        );
    }

    @Test
    void rejectsUnknownGuestRegionId() {
        GuestRegionService service = new GuestRegionService(loader);

        assertThrows(IllegalArgumentException.class, () -> service.getGuestRegion(999L));
    }
}
