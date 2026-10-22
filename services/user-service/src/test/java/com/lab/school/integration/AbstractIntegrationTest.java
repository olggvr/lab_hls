package com.lab.school.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Базовый класс интеграционных тестов: реальный PostgreSQL в Testcontainers + миграции Liquibase.
 * <p>
 * Контейнер один на все тестовые классы (singleton container pattern), чтобы Spring-контекст
 * переиспользовался между классами и не указывал на остановленную БД.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    /**
     * Версия базы зафиксирована точным тегом: тесты должны идти на той же версии PostgreSQL,
     * что указана в docker-compose.yml, а не на "последней подходящей".
     */
    static final String POSTGRES_IMAGE = "postgres:16.4-alpine";

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGRES_IMAGE);

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper objectMapper;

    // ---------------------------------------------------------------- HTTP helpers

    protected ResultActions postJson(String url, Object body) throws Exception {
        return mvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    protected ResultActions putJson(String url, Object body) throws Exception {
        return mvc.perform(put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    protected long createdId(ResultActions actions) throws Exception {
        MvcResult result = actions.andExpect(status().isCreated()).andReturn();
        return readLong(result, "$.id");
    }

    protected static long readLong(MvcResult result, String path) throws Exception {
        Number value = JsonPath.read(result.getResponse().getContentAsString(), path);
        return value.longValue();
    }

    protected static String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    protected static Map<String, Object> map(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    // ---------------------------------------------------------------- domain helpers

    protected long createUser(String role) throws Exception {
        String username = unique("u");
        return createdId(postJson("/api/users", map(
                "username", username,
                "fullName", "Test " + username,
                "email", username + "@test.local",
                "phone", "+79990000000",
                "role", role)));
    }

    protected long createSubject() throws Exception {
        return createdId(postJson("/api/subjects", map("name", unique("Subject "))));
    }

    protected long createClass(int year) throws Exception {
        return createdId(postJson("/api/classes", map("name", unique("C"), "year", year)));
    }

    protected long createTeacher(Long subjectId) throws Exception {
        long userId = createUser("TEACHER");
        return createdId(postJson("/api/teachers", map("userId", userId, "subjectId", subjectId)));
    }

    protected long createStudent(long classId) throws Exception {
        long userId = createUser("STUDENT");
        return createdId(postJson("/api/students", map("userId", userId, "classId", classId)));
    }

    protected long createParent() throws Exception {
        long userId = createUser("PARENT");
        return createdId(postJson("/api/parents", map("userId", userId)));
    }

    protected void linkParent(long studentId, long parentId, String relation) throws Exception {
        postJson("/api/students/" + studentId + "/parents", map("parentId", parentId, "relation", relation))
                .andExpect(status().isCreated());
    }

    protected long addWorkload(long teacherId, long classId, long subjectId) throws Exception {
        return createdId(postJson("/api/teachers/" + teacherId + "/workload",
                map("classId", classId, "subjectId", subjectId, "academicYear", "2026/2027")));
    }

    protected long userIdOfStudent(long studentId) throws Exception {
        return readLong(mvc.perform(get("/api/students/" + studentId)).andReturn(), "$.userId");
    }

    protected long userIdOfParent(long parentId) throws Exception {
        return readLong(mvc.perform(get("/api/parents/" + parentId)).andReturn(), "$.userId");
    }

    protected long userIdOfTeacher(long teacherId) throws Exception {
        return readLong(mvc.perform(get("/api/teachers/" + teacherId)).andReturn(), "$.userId");
    }
}
