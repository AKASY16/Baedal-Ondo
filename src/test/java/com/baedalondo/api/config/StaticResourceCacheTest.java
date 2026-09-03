package com.baedalondo.api.config;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 정적 리소스가 실제로 캐시되는지 확인한다.

 Spring Security가 넣는 no-store가 /css, /fonts, /images에도 걸려 있어서
 610KB짜리 폰트를 페이지마다 다시 받고 있었다. 캐시 정책이 사라지면 조용히 그 상태로 돌아가고,
 화면에서는 아무 증상도 보이지 않는다. 그래서 헤더를 테스트로 잡아 둔다.
 **/
@SpringBootTest
@AutoConfigureMockMvc
class StaticResourceCacheTest extends MySqlTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("폰트는 오래 캐시하고 no-store가 붙지 않는다")
    void cachesFontForALongTime() throws Exception {
        mockMvc.perform(get("/fonts/SUIT-Variable.woff2"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=31536000")))
                .andExpect(header().string("Cache-Control", containsString("immutable")))
                .andExpect(header().string("Cache-Control", not(containsString("no-store"))));
    }

    @Test
    @DisplayName("아이콘과 공유 이미지는 하루 캐시한다")
    void cachesImagesForADay() throws Exception {
        mockMvc.perform(get("/images/baedal-ondo-icon.svg"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=86400")))
                .andExpect(header().string("Cache-Control", not(containsString("no-store"))));
    }

    @Test
    @DisplayName("CSS는 저장은 하되 매번 다시 확인하게 둔다")
    void revalidatesCssInsteadOfStoringItForLong() throws Exception {
        mockMvc.perform(get("/css/common.css"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-cache")))
                .andExpect(header().string("Cache-Control", not(containsString("no-store"))))
                // 이 헤더가 있어야 다음 요청이 조건부로 나가고 바뀌지 않았으면 304로 끝난다.
                .andExpect(header().exists("Last-Modified"));
    }

    @Test
    @DisplayName("로그인 상태가 담기는 화면 응답은 계속 저장하지 않는다")
    void keepsRenderedPagesOutOfCaches() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")));
    }

}
