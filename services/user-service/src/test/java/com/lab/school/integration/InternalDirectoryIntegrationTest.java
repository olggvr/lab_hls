package com.lab.school.integration;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalDirectoryIntegrationTest extends AbstractIntegrationTest {

    @Test
    void resolvesLazyDirectoryRelationsOutsideRepositoryTransaction() throws Exception {
        long subjectId = createSubject();
        long classId = createClass(7);
        long teacherId = createTeacher(subjectId);
        long studentId = createStudent(classId);
        long parentId = createParent();
        long teacherUserId = userIdOfTeacher(teacherId);
        long studentUserId = userIdOfStudent(studentId);
        long parentUserId = userIdOfParent(parentId);
        addWorkload(teacherId, classId, subjectId);
        linkParent(studentId, parentId, "MOTHER");

        MvcResult assignment = postJson("/internal/directory/assignment",
                map("teacherId", teacherId, "classId", classId, "subjectId", subjectId))
                .andExpect(request().asyncStarted())
                .andReturn();
        mvc.perform(asyncDispatch(assignment))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teacherUserId").value(teacherUserId));

        MvcResult student = postJson("/internal/directory/student", map("studentId", studentId, "classId", classId))
                .andExpect(request().asyncStarted())
                .andReturn();
        mvc.perform(asyncDispatch(student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentUserId").value(studentUserId));

        MvcResult audience = mvc.perform(get("/internal/directory/grade-audience/{studentId}", studentId))
                .andExpect(request().asyncStarted())
                .andReturn();
        mvc.perform(asyncDispatch(audience))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipientIds[0]").value(studentUserId))
                .andExpect(jsonPath("$.recipientIds[1]").value(parentUserId));
    }
}
