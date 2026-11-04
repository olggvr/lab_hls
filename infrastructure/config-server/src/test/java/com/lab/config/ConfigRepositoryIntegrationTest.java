package com.lab.config;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "eureka.client.enabled=false",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false"
})
class ConfigRepositoryIntegrationTest {
    private static final Path REMOTE_REPOSITORY;
    private static final Path CLONE_DIRECTORY;

    static {
        try {
            Path testRoot = Files.createTempDirectory("config-server-test-");
            REMOTE_REPOSITORY = testRoot.resolve("remote");
            CLONE_DIRECTORY = testRoot.resolve("clone");
            writeRepositoryFiles(REMOTE_REPOSITORY);
            try (Git git = Git.init()
                    .setInitialBranch("main")
                    .setDirectory(REMOTE_REPOSITORY.toFile())
                    .call()) {
                git.add().addFilepattern(".").call();
                git.commit()
                        .setMessage("test configuration")
                        .setAuthor("Config Tests", "config-tests@school.local")
                        .call();
            }
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @DynamicPropertySource
    static void gitBackend(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.config.server.git.uri", () -> REMOTE_REPOSITORY.toUri().toString());
        registry.add("spring.cloud.config.server.git.basedir", CLONE_DIRECTORY::toString);
        registry.add("spring.cloud.config.server.git.default-label", () -> "main");
        registry.add("spring.cloud.config.server.git.clone-on-start", () -> true);
        registry.add("spring.cloud.config.server.git.force-pull", () -> true);
    }

    @Autowired
    private TestRestTemplate http;

    @Test
    void mergesCommonAndApplicationDirectoriesFromGitRepository() {
        ResponseEntity<Map> response = http.getForEntity("/assignment-service/default", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> propertySources = propertySources(response.getBody());
        assertThat(propertySources)
                .extracting(source -> source.get("name").toString())
                .anyMatch(name -> name.endsWith("assignment-service/application.yml"))
                .anyMatch(name -> name.endsWith("common/application.yml"));
        assertThat(value(propertySources, "server.port")).isEqualTo(8083);
        assertThat(value(propertySources, "eureka.client.service-url.defaultZone"))
                .isEqualTo("http://eureka:8761/eureka/");
    }

    @Test
    void servesGatewayRoutesFromGatewayDirectory() {
        ResponseEntity<Map> response = http.getForEntity("/api-gateway/default", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> propertySources = propertySources(response.getBody());
        assertThat(propertySources)
                .extracting(source -> source.get("name").toString())
                .anyMatch(name -> name.endsWith("api-gateway/application.yml"));
        assertThat(value(propertySources, "spring.cloud.gateway.server.webflux.routes[0].id"))
                .isEqualTo("assignment-service");
        assertThat(value(propertySources, "spring.cloud.gateway.server.webflux.routes[0].uri"))
                .isEqualTo("lb://assignment-service");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> propertySources(Map response) {
        assertThat(response).isNotNull();
        return (List<Map<String, Object>>) response.get("propertySources");
    }

    @SuppressWarnings("unchecked")
    private static Object value(List<Map<String, Object>> propertySources, String key) {
        return propertySources.stream()
                .map(source -> (Map<String, Object>) source.get("source"))
                .filter(source -> source.containsKey(key))
                .map(source -> source.get(key))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing configuration key: " + key));
    }

    private static void writeRepositoryFiles(Path repository) throws IOException {
        Path common = Files.createDirectories(repository.resolve("common"));
        Path assignment = Files.createDirectories(repository.resolve("assignment-service"));
        Path gateway = Files.createDirectories(repository.resolve("api-gateway"));

        Files.writeString(common.resolve("application.yml"), """
                eureka:
                  client:
                    service-url:
                      defaultZone: http://eureka:8761/eureka/
                """);
        Files.writeString(assignment.resolve("application.yml"), """
                server:
                  port: 8083
                spring:
                  r2dbc:
                    url: r2dbc:postgresql://assignment-db/assignments
                """);
        Files.writeString(gateway.resolve("application.yml"), """
                spring:
                  cloud:
                    gateway:
                      server:
                        webflux:
                          routes:
                            - id: assignment-service
                              uri: lb://assignment-service
                              predicates:
                                - Path=/api/assignments/**
                """);
    }
}
