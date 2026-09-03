package com.baedalondo.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new NoCacheInterceptor())
                .addPathPatterns("/dashboard/main", "/dashboard/main/**");
    }

    /**
     정적 리소스에 캐시 정책을 직접 준다.

     Spring Security는 Cache-Control이 비어 있는 응답에 no-store를 넣는다. 화면에는 그게 맞지만
     /css, /fonts, /images까지 같이 걸려서 610KB짜리 폰트를 페이지를 옮길 때마다 다시 받고 있었다.
     원본이 no-store를 주니 Cloudflare도 캐시를 포기하고(cf-cache-status: BYPASS) 전부 EC2에서 나갔다.

     핸들러가 먼저 Cache-Control을 넣으면 Security의 헤더 작성기는 그대로 두고 넘어간다.
     헤더는 응답이 커밋될 때 쓰이므로 순서상 이쪽이 먼저다.
     **/
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 폰트는 이 중 제일 크고 내용이 바뀌지 않는다. 파일명을 바꾸지 않는 한 다시 받을 이유가 없다.
        registry.addResourceHandler("/fonts/**")
                .addResourceLocations("classpath:/static/fonts/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365))
                        .cachePublic()
                        .immutable());

        // 아이콘과 공유 이미지는 가끔 바뀐다. 하루면 교체가 퍼진다.
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(1))
                        .cachePublic());

        // CSS는 배포마다 바뀔 수 있는데 파일명에 버전이 없다. 오래 캐시하면 배포 후에도
        // 옛 CSS가 남는다. 저장은 하게 두되 매번 물어보고, 그대로면 304로 끝낸다.
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCacheControl(CacheControl.noCache()
                        .cachePublic());
    }

}
