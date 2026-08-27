package com.baedalondo.api.home;

import com.baedalondo.api.dashboard.dto.DashboardView;
import com.baedalondo.api.dashboard.service.DashboardService;
import com.baedalondo.api.guest.domain.GuestRegion;
import com.baedalondo.api.guest.service.GuestRegionService;
import com.baedalondo.api.store.domain.Store;
import com.baedalondo.api.support.MySqlTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PublicEntryPageTest extends MySqlTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private GuestRegionService guestRegionService;

    @BeforeEach
    void setUpGuestDashboard() {
        GuestRegion region = guestRegion();
        when(guestRegionService.getGuestRegion(18L)).thenReturn(region);
        when(guestRegionService.getRegions()).thenReturn(List.of(region));
        when(dashboardService.getGuestDashboard(18L)).thenReturn(guestDashboard());
    }

    @Test
    @DisplayName("루트는 로그인으로 보내지 않고 공개 랜딩페이지를 렌더한다")
    void rootRendersPublicLandingPageWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(content().string(containsString("<h1 id=\"hero-title\">")))
                .andExpect(content().string(containsString(
                        "날씨·시간대·요일·대기환경·상권 데이터를 이용해<br>")))
                .andExpect(content().string(containsString("href=\"/dashboard/guest\"")))
                .andExpect(content().string(containsString("게스트로 바로 체험하기")))
                .andExpect(content().string(containsString("<div class=\"section-intro how-intro\">")))
                .andExpect(content().string(containsString("02 · START")));
    }

    @Test
    @DisplayName("랜딩 HTML에 검색·공유 메타와 WebSite 구조화 데이터가 들어간다")
    void landingPageContainsSeoMetadataInTheHtmlResponse() throws Exception {
        mockMvc.perform(get("/").header("Accept-Language", "en-US"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "ko-KR"))
                .andExpect(content().string(containsString(
                        "<title>날씨·상권 기반 배달 수요 환경 분석 - 배달온도</title>")))
                .andExpect(content().string(containsString(
                        "name=\"description\" content=\"날씨·시간대·요일·대기환경·상권 데이터를 이용해 현재 배달 수요 환경을 0~100점으로 보여주는 서비스입니다.\"")))
                .andExpect(content().string(containsString(
                        "rel=\"canonical\" href=\"https://www.baedalondo.com/\"")))
                .andExpect(content().string(containsString(
                        "property=\"og:title\" content=\"배달온도 - 지금의 배달 수요 환경을 한눈에\"")))
                .andExpect(content().string(containsString(
                        "name=\"robots\" content=\"index, follow\"")))
                .andExpect(content().string(containsString(
                        "rel=\"icon\" href=\"https://www.baedalondo.com/images/baedal-ondo-icon.svg\"")))
                .andExpect(content().string(containsString(
                        "<script type=\"application/ld+json\">")))
                .andExpect(content().string(containsString(
                        "\"@type\": \"WebSite\"")))
                .andExpect(content().string(containsString(
                        "\"url\": \"https://www.baedalondo.com/\"")))
                .andExpect(content().string(containsString(
                        "\"inLanguage\": \"ko-KR\"")))
                .andExpect(content().string(containsString(
                        "name=\"naver-site-verification\" content=\"91f202109aef7d0253dd27d1bde1067fbca22dcd\"")));
    }

    @Test
    @DisplayName("인증 페이지는 noindex이고 기능 경로는 계속 로그인으로 보호된다")
    void authenticationPagesAreNoIndexAndPrivateDashboardRemainsProtected() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "name=\"robots\" content=\"noindex, nofollow\"")));

        mockMvc.perform(get("/signup"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "name=\"robots\" content=\"noindex, nofollow\"")));

        for (String protectedPath : List.of(
                "/dashboard/main",
                "/dashboard/main/1",
                "/store/register",
                "/store/1/edit",
                "/api/private-probe",
                "/account/withdraw",
                "/logout"
        )) {
            mockMvc.perform(get(protectedPath))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(header().string("Location", containsString("/login")));
        }
    }

    @Test
    @DisplayName("등록되지 않은 공개 URL은 로그인으로 보내지 않고 404를 반환한다")
    void unknownPublicPathReturnsNotFoundWithoutLoginRedirect() throws Exception {
        mockMvc.perform(get("/seo-404-probe"))
                .andExpect(status().isNotFound())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(result -> assertNull(result.getRequest().getSession(false)));

        mockMvc.perform(get("/actuator/private-probe"))
                .andExpect(status().isNotFound())
                .andExpect(header().doesNotExist("Location"));
    }

    @Test
    @DisplayName("게스트 대시보드는 체험할 수 있지만 검색 색인에서는 제외한다")
    void guestDashboardIsPublicButNoIndex() throws Exception {
        mockMvc.perform(get("/dashboard/guest").param("regionId", "18"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(content().string(containsString(
                        "name=\"robots\" content=\"noindex, nofollow\"")))
                .andExpect(content().string(containsString(
                        "rel=\"canonical\" href=\"https://www.baedalondo.com/dashboard/guest\"")));
    }

    @Test
    @DisplayName("robots와 sitemap은 랜딩을 노출하고 인증 페이지를 제외한다")
    void robotsAndSitemapExposeOnlySearchablePublicEntries() throws Exception {
        mockMvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Allow: /")))
                .andExpect(content().string(containsString("Disallow: /dashboard/main")))
                .andExpect(content().string(containsString("Disallow: /api/")))
                .andExpect(content().string(not(containsString(
                        "Disallow: /dashboard/guest"))))
                .andExpect(content().string(containsString(
                        "Sitemap: https://www.baedalondo.com/sitemap.xml")));

        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "<loc>https://www.baedalondo.com/</loc>")))
                .andExpect(content().string(not(containsString(
                        "<loc>https://www.baedalondo.com/dashboard/guest</loc>"))))
                .andExpect(content().string(containsString(
                        "<loc>https://www.baedalondo.com/terms</loc>")))
                .andExpect(content().string(containsString(
                        "<loc>https://www.baedalondo.com/privacy</loc>")))
                .andExpect(content().string(not(containsString(
                        "<loc>https://www.baedalondo.com/login</loc>"))))
                .andExpect(content().string(not(containsString(
                        "<loc>https://www.baedalondo.com/signup</loc>"))));
    }

    private GuestRegion guestRegion() {
        return new GuestRegion(
                18L,
                "송파구청",
                "서울특별시 송파구 올림픽로 326 (신천동)",
                "서울특별시 송파구 올림픽로 326",
                "서울특별시 송파구 신천동 29-5 송파구청",
                "",
                "05552",
                "서울특별시",
                "송파구",
                "신천동",
                "1171010200",
                "117103123023",
                "1171010200100290005000269",
                "올림픽로",
                "0",
                "326",
                "0",
                62,
                126
        );
    }

    private DashboardView guestDashboard() {
        Store store = new Store(
                "송파구청",
                null,
                "서울특별시 송파구 올림픽로 326 (신천동)",
                "서울특별시 송파구 올림픽로 326",
                "서울특별시 송파구 신천동 29-5 송파구청",
                "",
                "05552",
                "서울특별시",
                "송파구",
                "신천동",
                "1171010200",
                "117103123023",
                "1171010200100290005000269",
                "올림픽로",
                "0",
                "326",
                "0",
                62,
                126
        );

        return new DashboardView(
                store,
                58,
                "보통 · 평균 수요 구간",
                "현재 배달 수요가 평소 수준입니다.",
                "•",
                "평소와 비슷한 주문 시간대",
                "•",
                "평소와 비슷한 요일 흐름",
                "•",
                "외출에 큰 불편이 없는 날씨",
                "•",
                "외출에 큰 불편이 없는 대기질",
                "대기질 정보",
                List.of()
        );
    }
}
