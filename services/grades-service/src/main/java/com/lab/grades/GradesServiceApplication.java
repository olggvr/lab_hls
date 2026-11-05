package com.lab.grades;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@OpenAPIDefinition(servers = @Server(url = "/", description = "Current host (API Gateway or direct service)"))
@EnableFeignClients
@SpringBootApplication
public class GradesServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(GradesServiceApplication.class, args);
    }
}
