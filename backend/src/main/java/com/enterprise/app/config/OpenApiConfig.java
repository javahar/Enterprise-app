package com.enterprise.app.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI enterpriseOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Enterprise App API")
                        .description("REST API for the Enterprise organizational hierarchy system")
                        .version("1.0.0"));
    }
}
