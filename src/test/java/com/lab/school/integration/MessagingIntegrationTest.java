package com.lab.school.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MessagingIntegrationTest extends AbstractIntegrationTest {

    private long teacherUserId;
    private long classId;
    private int year;
    private long studentA;
    private long studentB;
    private long parentOfBoth;

    @BeforeEach
    void setUp() throws Exception {
        teacherUserId = userIdOfTeacher(createTeacher(null));
        // отдельная параллель, чтобы рассылка по параллели была предсказуемой
        year = 11;
        classId = createClass(year);
        studentA = createStudent(classId);
        studentB = createStudent(classId);
        parentOfBoth = createParent();
        linkParent(studentA, parentOfBoth, "MOTHER");
        linkParent(studentB, parentOfBoth, "MOTHER");
    }

    @Test
    void directMessage_inbox_markRead_delete() throws Exception {
        long recipient = userIdOfStudent(studentA);
        MvcResult created = postJson("/api/messages", map(
                "senderId", teacherUserId,
                "recipientIds", List.of(recipient),
                "subject", "Привет",
                "body", "Не забудь тетрадь"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("DIRECT"))
                .andExpect(jsonPath("$.recipientsCount").value(1))
                .andReturn();
        long messageId = readLong(created, "$.id");

        mvc.perform(get("/api/messages/inbox").param("userId", String.valueOf(recipient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].messageId").value(messageId))
                .andExpect(jsonPath("$[0].read").value(false));

        mvc.perform(patch("/api/messages/" + messageId + "/read").param("userId", String.valueOf(recipient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true))
                .andExpect(jsonPath("$.readAt", notNullValue()));

        mvc.perform(patch("/api/messages/" + messageId + "/read").param("userId", String.valueOf(teacherUserId)))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/api/messages/" + messageId)).andExpect(status().isNoContent());
        mvc.perform(get("/api/messages/" + messageId)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/messages/" + messageId)).andExpect(status().isNotFound());
    }

    @Test
    void directMessage_unknownRecipient_returns404() throws Exception {
        postJson("/api/messages", map(
                "senderId", teacherUserId, "recipientIds", List.of(999999),
                "subject", "s", "body", "b"))
                .andExpect(status().isNotFound());
    }

    @Test
    void directMessage_emptyRecipients_returns400() throws Exception {
        postJson("/api/messages", map(
                "senderId", teacherUserId, "recipientIds", List.of(),
                "subject", "s", "body", "b"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void broadcastToClass_studentsAndParents_deduplicatesParent() throws Exception {
        // 2 ученика + 1 общий родитель = 3 получателя
        postJson("/api/messages/broadcast", map(
                "senderId", teacherUserId, "scope", "CLASS", "classId", classId,
                "audience", "ALL", "subject", "Собрание", "body", "В пятницу в 18:00"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("BROADCAST"))
                .andExpect(jsonPath("$.recipientsCount").value(3));
    }

    @Test
    void broadcastToClass_parentsOnly() throws Exception {
        postJson("/api/messages/broadcast", map(
                "senderId", teacherUserId, "scope", "CLASS", "classId", classId,
                "audience", "PARENTS", "subject", "Родителям", "body", "Текст"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recipientsCount").value(1));
    }

    @Test
    void broadcastToParallel_andSchool() throws Exception {
        postJson("/api/messages/broadcast", map(
                "senderId", teacherUserId, "scope", "PARALLEL", "year", year,
                "audience", "STUDENTS", "subject", "Параллель", "body", "Текст"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recipientsCount", greaterThanOrEqualTo(2)));

        postJson("/api/messages/broadcast", map(
                "senderId", teacherUserId, "scope", "SCHOOL",
                "audience", "ALL", "subject", "Всем", "body", "Каникулы!"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recipientsCount", greaterThanOrEqualTo(3)));
    }

    @Test
    void broadcast_validationAndBusinessRules() throws Exception {
        // ученик не может делать рассылку
        postJson("/api/messages/broadcast", map(
                "senderId", userIdOfStudent(studentA), "scope", "SCHOOL",
                "audience", "ALL", "subject", "s", "body", "b"))
                .andExpect(status().isUnprocessableEntity());

        // CLASS без classId / PARALLEL без year
        postJson("/api/messages/broadcast", map(
                "senderId", teacherUserId, "scope", "CLASS",
                "audience", "ALL", "subject", "s", "body", "b"))
                .andExpect(status().isUnprocessableEntity());
        postJson("/api/messages/broadcast", map(
                "senderId", teacherUserId, "scope", "PARALLEL",
                "audience", "ALL", "subject", "s", "body", "b"))
                .andExpect(status().isUnprocessableEntity());

        // пустой класс — нет получателей
        postJson("/api/messages/broadcast", map(
                "senderId", teacherUserId, "scope", "CLASS", "classId", createClass(1),
                "audience", "ALL", "subject", "s", "body", "b"))
                .andExpect(status().isUnprocessableEntity());

        // несуществующий класс
        postJson("/api/messages/broadcast", map(
                "senderId", teacherUserId, "scope", "CLASS", "classId", 999999,
                "audience", "ALL", "subject", "s", "body", "b"))
                .andExpect(status().isNotFound());

        // невалидное тело
        postJson("/api/messages/broadcast", map(
                "senderId", teacherUserId, "scope", "SCHOOL", "audience", "ALL", "subject", "", "body", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inbox_validation() throws Exception {
        mvc.perform(get("/api/messages/inbox")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/messages/inbox").param("userId", "999999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/messages/inbox").param("userId", String.valueOf(teacherUserId)).param("size", "60"))
                .andExpect(status().isBadRequest());
    }
}
