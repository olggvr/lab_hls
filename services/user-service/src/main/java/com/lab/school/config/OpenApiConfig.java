package com.lab.school.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI schoolOpenApi() {
        return new OpenAPI().info(new Info()
                .title("School Management System API")
                .version("1.0.0")
                .description("ЛР1 — монолит: учителя, ученики, родители, домашние задания, оценки, рассылки"));
    }
}
