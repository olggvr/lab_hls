package com.lab.grades;

import com.lab.grades.api.GradeRequest;
import com.lab.grades.integration.AssignmentClient;
import com.lab.grades.integration.GradeAudienceClient;
import com.lab.grades.integration.GradeAudienceResponse;
import com.lab.grades.integration.GradeScopeClient;
import com.lab.grades.integration.GradeScopeResponse;
import com.lab.grades.integration.GradingContextResponse;
import com.lab.grades.integration.MessageClient;
import com.lab.grades.integration.NotificationResponse;
import com.lab.grades.service.ReactiveGradeService;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class ReactiveGradeServiceIntegrationTest {
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.4-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    ReactiveGradeService service;

    @MockitoBean
    AssignmentClient assignmentClient;
    @MockitoBean
    GradeAudienceClient gradeAudienceClient;
    @MockitoBean
    GradeScopeClient gradeScopeClient;
    @MockitoBean
    MessageClient messageClient;

    @Test
    void jpaWorkRunsBehindReactiveApiAndSendsNotification() {
        when(assignmentClient.gradingContext(1L, 10L)).thenReturn(Mono.just(new GradingContextResponse(
                100L, 1L, 10L, "Student", "https://files.test/a.pdf", LocalDateTime.now(),
                20L, 2L, "Teacher", "Homework", "Math")));
        when(gradeAudienceClient.gradeAudience(10L)).thenReturn(new GradeAudienceResponse(
                10L, "Student", List.of(4L, 7L)));
        when(gradeScopeClient.gradeScope(4L)).thenReturn(new GradeScopeResponse("STUDENT", List.of(10L), null));
        when(messageClient.sendGradeNotification(any())).thenReturn(Mono.just(new NotificationResponse(500L, 2, true)));

        StepVerifier.create(service.grade(1L, new GradeRequest(10L, 5)))
                .assertNext(response -> {
                    assertThat(response.submission().grade()).isEqualTo(5);
                    assertThat(response.notificationId()).isEqualTo(500L);
                    assertThat(response.notifiedRecipients()).isEqualTo(2);
                })
                .verifyComplete();

        StepVerifier.create(service.listForUser(4L))
                .assertNext(entry -> assertThat(entry.grade()).isEqualTo(5))
                .verifyComplete();
    }

    @Test
    void rejectsInvalidDomainDataFromAssignmentService() {
        when(assignmentClient.gradingContext(2L, 11L)).thenReturn(Mono.just(new GradingContextResponse(
                101L, 2L, 11L, "", "https://files.test/b.pdf", LocalDateTime.now(),
                21L, 3L, "Teacher", "Homework", "Math")));
        when(gradeAudienceClient.gradeAudience(11L)).thenReturn(new GradeAudienceResponse(
                11L, "Student", List.of(5L)));

        StepVerifier.create(service.grade(2L, new GradeRequest(11L, 4)))
                .expectError(ConstraintViolationException.class)
                .verify();
    }
}
