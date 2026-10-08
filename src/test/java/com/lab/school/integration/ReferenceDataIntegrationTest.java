package com.lab.school.integration;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Справочники: предметы и классы.
 */
class ReferenceDataIntegrationTest extends AbstractIntegrationTest {

    @Test
    void subjectCrud() throws Exception {
        String name = unique("Химия ");
        long id = createdId(postJson("/api/subjects", map("name", name)));

        mvc.perform(get("/api/subjects/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name));

        postJson("/api/subjects", map("name", name)).andExpect(status().isConflict());

        String renamed = unique("Биология ");
        putJson("/api/subjects/" + id, map("name", renamed))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(renamed));

        mvc.perform(get("/api/subjects").param("size", "50")).andExpect(status().isOk());

        mvc.perform(delete("/api/subjects/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/subjects/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void subjectRenameToExisting_returns409() throws Exception {
        String existing = unique("Алгебра ");
        createdId(postJson("/api/subjects", map("name", existing)));
        long other = createSubject();
        putJson("/api/subjects/" + other, map("name", existing)).andExpect(status().isConflict());
    }

    @Test
    void subjectBlankName_returns400() throws Exception {
        postJson("/api/subjects", map("name", " ")).andExpect(status().isBadRequest());
    }

    @Test
    void classCrud() throws Exception {
        String name = unique("K");
        long id = createdId(postJson("/api/classes", map("name", name, "year", 9)));

        mvc.perform(get("/api/classes/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.year").value(9));

        postJson("/api/classes", map("name", name, "year", 9)).andExpect(status().isConflict());

        putJson("/api/classes/" + id, map("name", name, "year", 10))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(10));

        mvc.perform(get("/api/classes")).andExpect(status().isOk());

        mvc.perform(delete("/api/classes/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/classes/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void classRenameToExisting_returns409() throws Exception {
        String existing = unique("K");
        createdId(postJson("/api/classes", map("name", existing, "year", 3)));
        long other = createClass(3);
        putJson("/api/classes/" + other, map("name", existing, "year", 3)).andExpect(status().isConflict());
    }

    @Test
    void classInvalidYear_returns400() throws Exception {
        postJson("/api/classes", map("name", unique("K"), "year", 12))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.year").exists());
    }

    @Test
    void classStudents_andDeleteClassWithStudents_returns409() throws Exception {
        long classId = createClass(4);
        long studentId = createStudent(classId);

        mvc.perform(get("/api/classes/" + classId + "/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(studentId));

        mvc.perform(delete("/api/classes/" + classId)).andExpect(status().isConflict());
        mvc.perform(get("/api/classes/999999/students")).andExpect(status().isNotFound());
    }
}
