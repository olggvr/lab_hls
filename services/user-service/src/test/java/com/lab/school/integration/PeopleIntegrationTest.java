package com.lab.school.integration;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Учителя, ученики, родители и связи между ними.
 */
class PeopleIntegrationTest extends AbstractIntegrationTest {

    @Test
    void teacherCrudAndWorkload() throws Exception {
        long subjectId = createSubject();
        long otherSubjectId = createSubject();
        long classId = createClass(7);
        long teacherId = createTeacher(subjectId);

        mvc.perform(get("/api/teachers/" + teacherId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjectId").value(subjectId))
                .andExpect(jsonPath("$.fullName", notNullValue()));

        putJson("/api/teachers/" + teacherId, map("subjectId", otherSubjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjectId").value(otherSubjectId));

        long workloadId = addWorkload(teacherId, classId, subjectId);
        postJson("/api/teachers/" + teacherId + "/workload",
                map("classId", classId, "subjectId", subjectId, "academicYear", "2026/2027"))
                .andExpect(status().isConflict());
        postJson("/api/teachers/" + teacherId + "/workload",
                map("classId", classId, "subjectId", subjectId, "academicYear", "2026-27"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/teachers/" + teacherId + "/workload"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].academicYear").value("2026/2027"))
                .andExpect(jsonPath("$[0].classId").value(classId));

        mvc.perform(get("/api/teachers")).andExpect(status().isOk());

        mvc.perform(delete("/api/teachers/" + teacherId + "/workload/" + workloadId))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/teachers/" + teacherId + "/workload/" + workloadId))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/api/teachers/" + teacherId)).andExpect(status().isNoContent());
        mvc.perform(get("/api/teachers/" + teacherId)).andExpect(status().isNotFound());
    }

    @Test
    void teacherWithoutSubject_isAllowed() throws Exception {
        long teacherId = createTeacher(null);
        mvc.perform(get("/api/teachers/" + teacherId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjectId").doesNotExist());
    }

    @Test
    void profileForUserWithWrongRole_returns422() throws Exception {
        long studentUser = createUser("STUDENT");
        postJson("/api/teachers", map("userId", studentUser)).andExpect(status().isUnprocessableEntity());
        postJson("/api/parents", map("userId", studentUser)).andExpect(status().isUnprocessableEntity());
        long teacherUser = createUser("TEACHER");
        postJson("/api/students", map("userId", teacherUser, "classId", createClass(2)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void duplicateProfile_returns409() throws Exception {
        long userId = createUser("TEACHER");
        postJson("/api/teachers", map("userId", userId)).andExpect(status().isCreated());
        postJson("/api/teachers", map("userId", userId)).andExpect(status().isConflict());
    }

    @Test
    void profileForUnknownUser_returns404() throws Exception {
        postJson("/api/parents", map("userId", 999999)).andExpect(status().isNotFound());
    }

    @Test
    void studentCrudAndPageWithTotalCount() throws Exception {
        long classId = createClass(6);
        long otherClassId = createClass(6);
        long studentId = createStudent(classId);
        createStudent(classId);

        mvc.perform(get("/api/students/" + studentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classId").value(classId));

        MvcResult page = mvc.perform(get("/api/students/page").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Total-Count"))
                .andExpect(jsonPath("$.length()").value(1))
                .andReturn();
        long total = Long.parseLong(page.getResponse().getHeader("X-Total-Count"));
        assertTrue(total >= 2, "X-Total-Count должен учитывать всех учеников");

        mvc.perform(get("/api/students/page").param("size", "51")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/students")).andExpect(status().isOk());

        putJson("/api/students/" + studentId, map("classId", otherClassId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classId").value(otherClassId));

        mvc.perform(delete("/api/students/" + studentId)).andExpect(status().isNoContent());
        mvc.perform(get("/api/students/" + studentId)).andExpect(status().isNotFound());
    }

    @Test
    void parentLinksAndContacts() throws Exception {
        long classId = createClass(8);
        long studentId = createStudent(classId);
        long motherId = createParent();
        long fatherId = createParent();

        linkParent(studentId, motherId, "MOTHER");
        linkParent(studentId, fatherId, "FATHER");
        postJson("/api/students/" + studentId + "/parents", map("parentId", motherId, "relation", "MOTHER"))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/students/" + studentId + "/parents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].email", notNullValue()))
                .andExpect(jsonPath("$[0].phone", notNullValue()));

        mvc.perform(get("/api/parents/" + motherId + "/children"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].studentId").value(studentId))
                .andExpect(jsonPath("$[0].relation").value("MOTHER"));

        mvc.perform(get("/api/parents")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));

        mvc.perform(delete("/api/students/" + studentId + "/parents/" + fatherId))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/students/" + studentId + "/parents/" + fatherId))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/api/parents/" + motherId)).andExpect(status().isNoContent());
        mvc.perform(get("/api/students/" + studentId + "/parents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/parents/" + motherId)).andExpect(status().isNotFound());
    }

    @Test
    void linkParentWithInvalidRelation_returns400() throws Exception {
        long studentId = createStudent(createClass(1));
        long parentId = createParent();
        postJson("/api/students/" + studentId + "/parents", map("parentId", parentId, "relation", "UNCLE"))
                .andExpect(status().isBadRequest());
    }
}
