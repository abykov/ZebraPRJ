package com.example.ZebraPRJ.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;

public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI(){
        return new OpenAPI()
                .info(new Info()
                        .title("ZebraPRJ API")
                        .version("1.0.0")
                        .description("API documentation for Zebra App"));
    }
}
