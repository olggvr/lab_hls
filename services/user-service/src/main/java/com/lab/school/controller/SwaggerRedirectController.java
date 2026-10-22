package com.lab.school.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Короткий адрес Swagger UI: /swagger-ui -> /swagger-ui/index.html.
 */
@Hidden
@RestController
public class SwaggerRedirectController {

    private static final URI SWAGGER_UI = URI.create("/swagger-ui/index.html");

    @GetMapping({"/", "/swagger-ui"})
    public ResponseEntity<Void> redirectToSwaggerUi() {
        return ResponseEntity.status(HttpStatus.FOUND).location(SWAGGER_UI).build();
    }
}
