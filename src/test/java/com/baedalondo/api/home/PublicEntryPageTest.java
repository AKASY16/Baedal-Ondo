package com.baedalondo.api.home;

import com.baedalondo.api.support.MySqlTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PublicEntryPageTest extends MySqlTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("루트는 로그인으로 보내지 않고 공개 랜딩페이지를 렌더한다")
    void rootRendersPublicLandingPageWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(content().string(containsString("<h1 id=\"hero-title\">")))
                .andExpect(content().string(containsString("날씨·시간대·요일·대기환경·상권 데이터를 이용해")))
                .andExpect(content().string(containsString("href=\"/dashboard/guest\"")))
                .andExpect(content().string(containsString("게스트로 바로 체험하기")));
    }

    @Test
    @DisplayName("랜딩 HTML에 canonical, Open Graph, 네이버 소유확인 메타가 들어간다")
    void landingPageContainsSeoMetadataInTheHtmlResponse() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
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
                        "name=\"naver-site-verification\" content=\"91f202109aef7d0253dd27d1bde1067fbca22dcd\"")));
    }

    @Test
    @DisplayName("인증 페이지는 noindex이고 로그인 대시보드는 계속 보호된다")
    void authenticationPagesAreNoIndexAndPrivateDashboardRemainsProtected() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "name=\"robots\" content=\"noindex, nofollow\"")));

        mockMvc.perform(get("/signup"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "name=\"robots\" content=\"noindex, nofollow\"")));

        mockMvc.perform(get("/dashboard/main"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login")));
    }

    @Test
    @DisplayName("robots와 sitemap은 랜딩을 노출하고 인증 페이지를 제외한다")
    void robotsAndSitemapExposeOnlySearchablePublicEntries() throws Exception {
        mockMvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Allow: /")))
                .andExpect(content().string(containsString("Disallow: /dashboard/main")))
                .andExpect(content().string(containsString(
                        "Sitemap: https://www.baedalondo.com/sitemap.xml")));

        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "<loc>https://www.baedalondo.com/</loc>")))
                .andExpect(content().string(containsString(
                        "<loc>https://www.baedalondo.com/dashboard/guest</loc>")))
                .andExpect(content().string(not(containsString(
                        "<loc>https://www.baedalondo.com/login</loc>"))))
                .andExpect(content().string(not(containsString(
                        "<loc>https://www.baedalondo.com/signup</loc>"))));
    }
}
