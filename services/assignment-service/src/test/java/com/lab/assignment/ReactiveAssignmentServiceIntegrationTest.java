package com.lab.assignment;

import com.lab.assignment.api.AssignmentCreateRequest;
import com.lab.assignment.api.AssignmentUpdateRequest;
import com.lab.assignment.api.SubmissionCreateRequest;
import com.lab.assignment.integration.AssignmentReferenceResponse;
import com.lab.assignment.integration.StudentReferenceResponse;
import com.lab.assignment.integration.UserClient;
import com.lab.assignment.service.ReactiveAssignmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import reactor.test.StepVerifier;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class ReactiveAssignmentServiceIntegrationTest {
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.4-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://" + POSTGRES.getHost() + ":"
                + POSTGRES.getMappedPort(5432) + "/" + POSTGRES.getDatabaseName());
        registry.add("spring.r2dbc.username", POSTGRES::getUsername);
        registry.add("spring.r2dbc.password", POSTGRES::getPassword);
    }

    @Autowired
    ReactiveAssignmentService service;

    @MockitoBean
    UserClient userClient;

    @Test
    void assignmentAndSubmissionAreStoredReactively() {
        when(userClient.resolveAssignment(any())).thenReturn(new AssignmentReferenceResponse(
                11L, 21L, "Teacher", 31L, "7A", 41L, "Math"));
        when(userClient.resolveStudent(any())).thenReturn(new StudentReferenceResponse(
                51L, 61L, "Student", 31L));

        StepVerifier.create(service.create(new AssignmentCreateRequest(
                        11L, 31L, 41L, "Homework", "Description", LocalDate.now().plusDays(1)))
                .flatMap(assignment -> service.submit(assignment.id(),
                                new SubmissionCreateRequest(51L, "https://files.test/solution.pdf"))
                        .flatMap(submission -> service.gradingContext(assignment.id(), 51L))))
                .assertNext(context -> {
                    assertThat(context.teacherUserId()).isEqualTo(21L);
                    assertThat(context.studentId()).isEqualTo(51L);
                    assertThat(context.assignmentTitle()).isEqualTo("Homework");
                })
                .verifyComplete();
    }

    @Test
    void supportsPagingUpdatingListingAndDeletingAssignments() {
        when(userClient.resolveAssignment(any())).thenReturn(new AssignmentReferenceResponse(
                12L, 22L, "Second Teacher", 32L, "8B", 42L, "Physics"));
        when(userClient.resolveStudent(any())).thenReturn(new StudentReferenceResponse(
                52L, 62L, "Second Student", 32L));

        var first = service.create(new AssignmentCreateRequest(
                12L, 32L, 42L, "First", "Description", LocalDate.now().plusDays(2))).block();
        var second = service.create(new AssignmentCreateRequest(
                12L, 32L, 42L, "Second", "Description", LocalDate.now().plusDays(3))).block();

        assertThat(first).isNotNull();
        assertThat(second).isNotNull();
        assertThat(service.get(first.id()).block().title()).isEqualTo("First");
        assertThat(service.scroll(null, 1).block().hasNext()).isTrue();
        assertThat(service.scroll(second.id(), 50).block().items()).isNotEmpty();
        assertThat(service.page(-1, 100).block().total()).isGreaterThanOrEqualTo(2);

        var updated = service.update(first.id(), new AssignmentUpdateRequest(
                "Updated", "New description", LocalDate.now().plusDays(4))).block();
        assertThat(updated.title()).isEqualTo("Updated");

        var submission = service.submit(first.id(),
                new SubmissionCreateRequest(52L, "https://files.test/second.pdf")).block();
        assertThat(service.listSubmissions(first.id()).collectList().block())
                .extracting("id").contains(submission.id());

        service.delete(first.id()).block();
        StepVerifier.create(service.get(first.id()))
                .expectErrorMessage("Домашнее задание id=" + first.id() + " не найдено")
                .verify();
    }
}
