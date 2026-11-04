package com.lab.assignment;

import com.lab.assignment.api.AssignmentCreateRequest;
import com.lab.assignment.api.AssignmentUpdateRequest;
import com.lab.assignment.api.SubmissionCreateRequest;
import com.lab.assignment.integration.AssignmentReferenceResponse;
import com.lab.assignment.integration.StudentReferenceResponse;
import com.lab.assignment.integration.UserClient;
import com.lab.assignment.service.AssignmentService;
import com.lab.assignment.service.SubmissionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import reactor.test.StepVerifier;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class AssignmentServicesIntegrationTest {
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
    AssignmentService assignmentService;

    @Autowired
    SubmissionService submissionService;

    @MockitoBean
    UserClient userClient;

    @Test
    void assignmentAndSubmissionAreStoredReactively() {
        when(userClient.resolveAssignment(any())).thenReturn(Mono.just(new AssignmentReferenceResponse(
                11L, 21L, "Teacher", 31L, "7A", 41L, "Math")));
        when(userClient.resolveStudent(any())).thenReturn(Mono.just(new StudentReferenceResponse(
                51L, 61L, "Student", 31L)));

        StepVerifier.create(assignmentService.create(new AssignmentCreateRequest(
                        11L, 31L, 41L, "Homework", "Description", LocalDate.now().plusDays(1)))
                .flatMap(assignment -> submissionService.submit(assignment.id(),
                                new SubmissionCreateRequest(51L, "https://files.test/solution.pdf"))
                        .flatMap(submission -> submissionService.gradingContext(assignment.id(), 51L))))
                .assertNext(context -> {
                    assertThat(context.teacherUserId()).isEqualTo(21L);
                    assertThat(context.studentId()).isEqualTo(51L);
                    assertThat(context.assignmentTitle()).isEqualTo("Homework");
                })
                .verifyComplete();
    }

    @Test
    void supportsPagingUpdatingListingAndDeletingAssignments() {
        when(userClient.resolveAssignment(any())).thenReturn(Mono.just(new AssignmentReferenceResponse(
                12L, 22L, "Second Teacher", 32L, "8B", 42L, "Physics")));
        when(userClient.resolveStudent(any())).thenReturn(Mono.just(new StudentReferenceResponse(
                52L, 62L, "Second Student", 32L)));

        var first = assignmentService.create(new AssignmentCreateRequest(
                12L, 32L, 42L, "First", "Description", LocalDate.now().plusDays(2))).block();
        var second = assignmentService.create(new AssignmentCreateRequest(
                12L, 32L, 42L, "Second", "Description", LocalDate.now().plusDays(3))).block();

        assertThat(first).isNotNull();
        assertThat(second).isNotNull();
        assertThat(assignmentService.get(first.id()).block().title()).isEqualTo("First");
        assertThat(assignmentService.scroll(null, 1).block().hasNext()).isTrue();
        assertThat(assignmentService.scroll(second.id(), 50).block().items()).isNotEmpty();
        assertThat(assignmentService.page(-1, 100).block().total()).isGreaterThanOrEqualTo(2);

        var updated = assignmentService.update(first.id(), new AssignmentUpdateRequest(
                "Updated", "New description", LocalDate.now().plusDays(4))).block();
        assertThat(updated.title()).isEqualTo("Updated");

        var submission = submissionService.submit(first.id(),
                new SubmissionCreateRequest(52L, "https://files.test/second.pdf")).block();
        assertThat(submissionService.list(first.id()).collectList().block())
                .extracting("id").contains(submission.id());

        assignmentService.delete(first.id()).block();
        StepVerifier.create(assignmentService.get(first.id()))
                .expectErrorMessage("Домашнее задание id=" + first.id() + " не найдено")
                .verify();
    }

    @Test
    void rejectsDuplicateSubmissionWithoutBreakingReactiveTransaction() {
        when(userClient.resolveAssignment(any())).thenReturn(Mono.just(new AssignmentReferenceResponse(
                13L, 23L, "Teacher", 33L, "9A", 43L, "Chemistry")));
        when(userClient.resolveStudent(any())).thenReturn(Mono.just(new StudentReferenceResponse(
                53L, 63L, "Student", 33L)));

        var assignment = assignmentService.create(new AssignmentCreateRequest(
                13L, 33L, 43L, "Lab", "Description", LocalDate.now().plusDays(5))).block();
        var request = new SubmissionCreateRequest(53L, "https://files.test/duplicate.pdf");
        assertThat(assignment).isNotNull();
        submissionService.submit(assignment.id(), request).block();

        StepVerifier.create(submissionService.submit(assignment.id(), request))
                .expectErrorMessage("Ученик уже отправил решение этого задания")
                .verify();
        assertThat(submissionService.list(assignment.id()).collectList().block()).hasSize(1);
    }

    @Test
    void reportsMissingAssignmentAndMissingStudentSubmission() {
        when(userClient.resolveAssignment(any())).thenReturn(Mono.just(new AssignmentReferenceResponse(
                14L, 24L, "Teacher", 34L, "10A", 44L, "Biology")));
        var assignment = assignmentService.create(new AssignmentCreateRequest(
                14L, 34L, 44L, "Essay", null, LocalDate.now().plusDays(6))).block();
        assertThat(assignment).isNotNull();

        StepVerifier.create(submissionService.submit(Long.MAX_VALUE,
                        new SubmissionCreateRequest(54L, "https://files.test/missing.pdf")))
                .expectErrorMessage("Домашнее задание id=" + Long.MAX_VALUE + " не найдено")
                .verify();
        StepVerifier.create(submissionService.gradingContext(assignment.id(), 999L))
                .expectErrorMessage("Решение ученика id=999 по заданию id=" + assignment.id() + " не найдено")
                .verify();
    }
}
