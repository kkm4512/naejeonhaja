package com.example.naejeonhaja.common.config;

import com.example.naejeonhaja.config.AppProperties;
import com.example.naejeonhaja.domain.inhouse.auth.GroupAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * docker compose 로 띄우면 프론트 서버가 /api 를 프록시하므로 브라우저 입장에서는 같은 출처다.
 * 프론트를 별도 도메인에 배포할 때만 APP_ALLOWED_ORIGINS 에 그 도메인을 넣으면 된다.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties.App appProperties;
    private final GroupAuthInterceptor groupAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 방 안의 데이터는 입장 토큰이 있어야 볼 수 있다 (입장 API 자체는 제외)
        registry.addInterceptor(groupAuthInterceptor)
                .addPathPatterns("/api/v1/groups/*", "/api/v1/groups/*/**")
                .excludePathPatterns("/api/v1/groups/*/join");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = appProperties.allowedOrigins().toArray(String[]::new);
        registry.addMapping("/api/**")
                .allowedOriginPatterns(origins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
        registry.addMapping("/health")
                .allowedOriginPatterns(origins)
                .allowedMethods("GET", "OPTIONS")
                .maxAge(3600);
    }
}
