package com.creatorhub.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 元信息。业务端点将在各限界上下文的 Controller 上以注解补充。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI creatorHubOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("CreatorHub API")
                        .version("0.1.0")
                        .description("CreatorHub modular monolith REST contract. "
                                + "API surface mirrors docs/openapi.yaml as the single source of truth."))
                .components(new Components());
    }
}
