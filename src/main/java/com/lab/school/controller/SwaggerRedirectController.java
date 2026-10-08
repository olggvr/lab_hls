package com.lab.school.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Короткий адрес Swagger UI: /swagger-ui -> /swagger-ui/index.html.
 */
@Hidden
@Controller
public class SwaggerRedirectController {

    @GetMapping({"/", "/swagger-ui"})
    public String swaggerUi() {
        return "redirect:/swagger-ui/index.html";
    }
}
