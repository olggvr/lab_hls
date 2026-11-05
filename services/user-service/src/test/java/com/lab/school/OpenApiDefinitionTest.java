package com.lab.school;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OpenApiDefinitionTest {
    @Test
    void usesCurrentHostSoGatewaySwaggerDoesNotExposeContainerAddress() {
        OpenAPIDefinition definition = SchoolApplication.class.getAnnotation(OpenAPIDefinition.class);

        assertNotNull(definition);
        assertEquals("/", definition.servers()[0].url());
    }
}
