package com.lab.assignment.service;

import com.lab.assignment.api.GradingContextResponse;
import com.lab.assignment.api.SubmissionCreateRequest;
import com.lab.assignment.api.SubmissionResponse;
import com.lab.assignment.domain.AssignmentEntity;
import com.lab.assignment.domain.SubmissionEntity;
import com.lab.assignment.exception.ConflictException;
import com.lab.assignment.exception.NotFoundException;
import com.lab.assignment.integration.StudentReferenceRequest;
import com.lab.assignment.integration.UserClient;
import com.lab.assignment.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SubmissionService {
    private final SubmissionRepository submissionRepository;
    private final AssignmentService assignmentService;
    private final UserClient userClient;
    private final TransactionalOperator transactionalOperator;

    public Mono<SubmissionResponse> submit(Long assignmentId, SubmissionCreateRequest request) {
        return assignmentService.requireAssignment(assignmentId)
                .flatMap(assignment -> userClient.resolveStudent(
                                new StudentReferenceRequest(request.studentId(), assignment.getClassId()))
                        .flatMap(student -> submissionRepository.existsByAssignmentIdAndStudentId(
                                        assignmentId, request.studentId())
                                .flatMap(exists -> exists
                                        ? Mono.error(new ConflictException("Ученик уже отправил решение этого задания"))
                                        : submissionRepository.save(new SubmissionEntity(
                                                null, assignmentId, student.studentId(), student.studentUserId(),
                                                student.studentName(), request.contentUrl(), LocalDateTime.now(),
                                                "SUBMITTED")))))
                .map(this::toResponse)
                .as(transactionalOperator::transactional);
    }

    public Flux<SubmissionResponse> list(Long assignmentId) {
        return assignmentService.requireAssignment(assignmentId)
                .thenMany(submissionRepository.findByAssignmentIdOrderByIdAsc(assignmentId))
                .map(this::toResponse);
    }

    public Mono<GradingContextResponse> gradingContext(Long assignmentId, Long studentId) {
        return Mono.zip(assignmentService.requireAssignment(assignmentId),
                        submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId)
                                .switchIfEmpty(Mono.error(new NotFoundException(
                                        "Решение ученика id=" + studentId + " по заданию id=" + assignmentId
                                                + " не найдено"))))
                .map(tuple -> toGradingContext(tuple.getT1(), tuple.getT2()));
    }

    private GradingContextResponse toGradingContext(AssignmentEntity assignment, SubmissionEntity submission) {
        return new GradingContextResponse(
                submission.getId(), assignment.getId(), submission.getStudentId(),
                submission.getStudentName(), submission.getContentUrl(), submission.getSubmittedAt(),
                assignment.getTeacherId(), assignment.getTeacherUserId(), assignment.getTeacherName(),
                assignment.getTitle(), assignment.getSubjectName());
    }

    private SubmissionResponse toResponse(SubmissionEntity entity) {
        return new SubmissionResponse(entity.getId(), entity.getAssignmentId(), entity.getStudentId(),
                entity.getStudentName(), entity.getContentUrl(), entity.getSubmittedAt(), null,
                entity.getStatus(), null);
    }
}
