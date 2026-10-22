package com.lab.school.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createUser_returns201_withLocationHeader() throws Exception {
        String username = unique("user");
        postJson("/api/users", map(
                "username", username,
                "fullName", "Иван Иванов",
                "email", username + "@test.local",
                "role", "TEACHER"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/users/")))
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.role").value("TEACHER"))
                .andExpect(jsonPath("$.roles[0]").value("TEACHER"));
    }

    @Test
    void createUser_duplicateUsername_returns409() throws Exception {
        String username = unique("dup");
        Object body = map("username", username, "fullName", "A",
                "email", username + "@test.local", "role", "STUDENT");
        postJson("/api/users", body).andExpect(status().isCreated());
        postJson("/api/users", map("username", username, "fullName", "A",
                "email", unique("other") + "@test.local", "role", "STUDENT"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void createUser_duplicateEmail_returns409() throws Exception {
        String email = unique("mail") + "@test.local";
        postJson("/api/users", map("username", unique("a"), "fullName", "A",
                "email", email, "role", "STUDENT")).andExpect(status().isCreated());
        postJson("/api/users", map("username", unique("b"), "fullName", "B",
                "email", email, "role", "STUDENT")).andExpect(status().isConflict());
    }

    @Test
    void createUser_invalidBody_returns400WithFieldErrors() throws Exception {
        postJson("/api/users", map(
                "username", "x",
                "fullName", "",
                "email", "not-an-email",
                "phone", "abc",
                "role", "TEACHER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.username").exists())
                .andExpect(jsonPath("$.fieldErrors.fullName").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.phone").exists());
    }

    @Test
    void createUser_unknownRole_returns400() throws Exception {
        postJson("/api/users", map("username", unique("r"), "fullName", "A",
                "email", unique("r") + "@test.local", "role", "DIRECTOR"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedJson_returns400() throws Exception {
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("{broken"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUnknownUser_returns404() throws Exception {
        mvc.perform(get("/api/users/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/api/users/999999"));
    }

    @Test
    void invalidPathVariable_returns400() throws Exception {
        mvc.perform(get("/api/users/abc")).andExpect(status().isBadRequest());
    }

    @Test
    void unknownEndpoint_returns404() throws Exception {
        mvc.perform(get("/api/unknown")).andExpect(status().isNotFound());
    }

    @Test
    void wrongHttpMethod_returns405() throws Exception {
        mvc.perform(post("/api/users/1")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void listUsers_pageSizeOver50_returns400() throws Exception {
        mvc.perform(get("/api/users").param("size", "51")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/users").param("page", "-1")).andExpect(status().isBadRequest());
    }

    @Test
    void listUsers_returnsAtMostRequestedSize() throws Exception {
        createUser("STUDENT");
        createUser("STUDENT");
        mvc.perform(get("/api/users").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", lessThanOrEqualTo(50)));
    }

    @Test
    void updateUser_changesContacts() throws Exception {
        long id = createUser("PARENT");
        String email = unique("new") + "@test.local";
        putJson("/api/users/" + id, map("fullName", "Новое Имя", "email", email, "phone", "+7 (999) 111-22-33"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Новое Имя"))
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void updateUser_emailTakenByAnother_returns409() throws Exception {
        long first = createUser("PARENT");
        long second = createUser("PARENT");
        String firstEmail = JsonPath.read(
                mvc.perform(get("/api/users/" + first)).andReturn().getResponse().getContentAsString(), "$.email");
        putJson("/api/users/" + second, map("fullName", "X", "email", firstEmail))
                .andExpect(status().isConflict());
    }

    @Test
    void addAndRemoveRole() throws Exception {
        long id = createUser("TEACHER");
        postJson("/api/users/" + id + "/roles", map("role", "PARENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", containsInAnyOrder("TEACHER", "PARENT")));
        postJson("/api/users/" + id + "/roles", map("role", "PARENT"))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/users/" + id + "/roles/PARENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", containsInAnyOrder("TEACHER")));
        mvc.perform(delete("/api/users/" + id + "/roles/PARENT"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(delete("/api/users/" + id + "/roles/TEACHER"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void deleteUser_returns204_thenNotFound() throws Exception {
        long id = createUser("STUDENT");
        mvc.perform(delete("/api/users/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/users/" + id)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/users/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void deleteUserWithProfile_returns409() throws Exception {
        long classId = createClass(5);
        long studentId = createStudent(classId);
        long userId = userIdOfStudent(studentId);
        mvc.perform(delete("/api/users/" + userId)).andExpect(status().isConflict());
    }

    @Test
    void swaggerUi_isAvailable() throws Exception {
        mvc.perform(get("/swagger-ui"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/swagger-ui/index.html")));
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/users']").exists())
                .andExpect(jsonPath("$.paths['/api/students']").exists());
    }
}
