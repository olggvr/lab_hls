package com.lab.school.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Сквозной сценарий: ДЗ -> решение -> оценка (+ уведомление) -> просмотр оценок.
 */
class AssignmentFlowIntegrationTest extends AbstractIntegrationTest {

    private long subjectId;
    private long classId;
    private long teacherId;
    private long studentId;

    @BeforeEach
    void setUp() throws Exception {
        subjectId = createSubject();
        classId = createClass(7);
        teacherId = createTeacher(subjectId);
        addWorkload(teacherId, classId, subjectId);
        studentId = createStudent(classId);
    }

    @Test
    void createAssignment_withoutWorkload_returns422() throws Exception {
        long otherClass = createClass(7);
        postJson("/api/assignments", map(
                "teacherId", teacherId, "classId", otherClass, "subjectId", subjectId,
                "title", "ДЗ", "dueDate", LocalDate.now().plusDays(1).toString()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void createAssignment_withPastDueDate_returns400() throws Exception {
        postJson("/api/assignments", map(
                "teacherId", teacherId, "classId", classId, "subjectId", subjectId,
                "title", "ДЗ", "dueDate", LocalDate.now().minusDays(1).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.dueDate").exists());
    }

    @Test
    void createAssignment_unknownTeacher_returns404() throws Exception {
        postJson("/api/assignments", map(
                "teacherId", 999999, "classId", classId, "subjectId", subjectId,
                "title", "ДЗ", "dueDate", LocalDate.now().plusDays(1).toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void assignmentCrud() throws Exception {
        long id = createAssignment(teacherId, classId, subjectId);

        mvc.perform(get("/api/assignments/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teacherId").value(teacherId))
                .andExpect(jsonPath("$.classId").value(classId))
                .andExpect(jsonPath("$.subjectId").value(subjectId))
                .andExpect(jsonPath("$.teacherName", notNullValue()));

        String due = LocalDate.now().plusDays(10).toString();
        putJson("/api/assignments/" + id, map("title", "Новое название", "description", "upd", "dueDate", due))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Новое название"))
                .andExpect(jsonPath("$.dueDate").value(due));

        mvc.perform(delete("/api/assignments/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/assignments/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void infiniteScroll_usesCursorWithoutTotal() throws Exception {
        long first = createAssignment(teacherId, classId, subjectId);
        long second = createAssignment(teacherId, classId, subjectId);
        long third = createAssignment(teacherId, classId, subjectId);

        // Начинаем "сверху" от только что созданных записей
        MvcResult page1 = mvc.perform(get("/api/assignments")
                        .param("cursor", String.valueOf(third + 1))
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("X-Total-Count"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].id").value(third))
                .andExpect(jsonPath("$.items[1].id").value(second))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andReturn();
        long nextCursor = readLong(page1, "$.nextCursor");
        assertEquals(second, nextCursor);

        mvc.perform(get("/api/assignments").param("cursor", String.valueOf(nextCursor)).param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(first));

        // Самый первый запрос — без курсора
        mvc.perform(get("/api/assignments").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));

        // Конец ленты
        mvc.perform(get("/api/assignments").param("cursor", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());

        mvc.perform(get("/api/assignments").param("size", "100")).andExpect(status().isBadRequest());
    }

    @Test
    void pagedList_hasTotalCountHeader() throws Exception {
        createAssignment(teacherId, classId, subjectId);
        createAssignment(teacherId, classId, subjectId);

        MvcResult result = mvc.perform(get("/api/assignments/page").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Total-Count"))
                .andExpect(jsonPath("$.length()").value(1))
                .andReturn();
        assertTrue(Long.parseLong(result.getResponse().getHeader("X-Total-Count")) >= 2);
    }

    @Test
    void submitAndGrade_notifiesStudentAndParents_atomically() throws Exception {
        long parentId = createParent();
        linkParent(studentId, parentId, "GUARDIAN");
        long assignmentId = createAssignment(teacherId, classId, subjectId);

        postJson("/api/assignments/" + assignmentId + "/submissions",
                map("studentId", studentId, "contentUrl", "https://files.test/solution.pdf"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.grade").doesNotExist());

        // повторная отправка
        postJson("/api/assignments/" + assignmentId + "/submissions",
                map("studentId", studentId, "contentUrl", "https://files.test/solution2.pdf"))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/assignments/" + assignmentId + "/submissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        MvcResult graded = postJson("/api/assignments/" + assignmentId + "/grade",
                map("studentId", studentId, "grade", 5))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submission.grade").value(5))
                .andExpect(jsonPath("$.submission.status").value("GRADED"))
                .andExpect(jsonPath("$.submission.gradedAt", notNullValue()))
                .andExpect(jsonPath("$.notifiedRecipients").value(2))
                .andReturn();
        long notificationId = readLong(graded, "$.notificationId");

        mvc.perform(get("/api/messages/" + notificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("GRADE_NOTIFICATION"))
                .andExpect(jsonPath("$.senderId").value(userIdOfTeacher(teacherId)));

        long parentUserId = userIdOfParent(parentId);
        mvc.perform(get("/api/messages/inbox").param("userId", String.valueOf(parentUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].messageId").value(notificationId))
                .andExpect(jsonPath("$[0].read").value(false));

        // Оценки: ученик видит свои, родитель — ребёнка, учитель — по своим заданиям
        long studentUserId = userIdOfStudent(studentId);
        mvc.perform(get("/api/users/" + studentUserId + "/grades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].grade").value(5))
                .andExpect(jsonPath("$[0].assignmentId").value(assignmentId));
        mvc.perform(get("/api/users/" + parentUserId + "/grades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value(studentId));
        mvc.perform(get("/api/users/" + userIdOfTeacher(teacherId) + "/grades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].studentId", containsInAnyOrder((int) studentId)));
    }

    @Test
    void grade_withoutSubmission_returns404_andInvalidGrade_returns400() throws Exception {
        long assignmentId = createAssignment(teacherId, classId, subjectId);
        postJson("/api/assignments/" + assignmentId + "/grade", map("studentId", studentId, "grade", 4))
                .andExpect(status().isNotFound());
        postJson("/api/assignments/" + assignmentId + "/grade", map("studentId", studentId, "grade", 6))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submit_byStudentFromAnotherClass_returns422() throws Exception {
        long assignmentId = createAssignment(teacherId, classId, subjectId);
        long stranger = createStudent(createClass(7));
        postJson("/api/assignments/" + assignmentId + "/submissions",
                map("studentId", stranger, "contentUrl", "https://files.test/x.pdf"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void submit_withInvalidUrl_returns400() throws Exception {
        long assignmentId = createAssignment(teacherId, classId, subjectId);
        postJson("/api/assignments/" + assignmentId + "/submissions",
                map("studentId", studentId, "contentUrl", "not a url"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.contentUrl").exists());
    }

    @Test
    void deleteTeacherWithAssignments_returns409() throws Exception {
        createAssignment(teacherId, classId, subjectId);
        mvc.perform(delete("/api/teachers/" + teacherId)).andExpect(status().isConflict());
    }
}
