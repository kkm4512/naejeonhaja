package com.example.naejeonhaja.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("내전하자 API")
                        .description("리그 오브 레전드 내전 팀 구성 플랫폼")
                        .version("0.1.0"));
    }
}
