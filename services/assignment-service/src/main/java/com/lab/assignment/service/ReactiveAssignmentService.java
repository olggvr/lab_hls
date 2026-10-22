package com.lab.assignment.service;

import com.lab.assignment.api.AssignmentCreateRequest;
import com.lab.assignment.api.AssignmentResponse;
import com.lab.assignment.api.AssignmentUpdateRequest;
import com.lab.assignment.api.CursorPage;
import com.lab.assignment.api.GradingContextResponse;
import com.lab.assignment.api.SubmissionCreateRequest;
import com.lab.assignment.api.SubmissionResponse;
import com.lab.assignment.domain.AssignmentEntity;
import com.lab.assignment.domain.SubmissionEntity;
import com.lab.assignment.exception.ConflictException;
import com.lab.assignment.exception.NotFoundException;
import com.lab.assignment.integration.AssignmentReferenceRequest;
import com.lab.assignment.integration.UserClient;
import com.lab.assignment.integration.StudentReferenceRequest;
import com.lab.assignment.repository.AssignmentRepository;
import com.lab.assignment.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReactiveAssignmentService {
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final UserClient userClient;
    private final TransactionalOperator transactionalOperator;

    public Mono<CursorPage<AssignmentResponse>> scroll(Long cursor, int size) {
        int safeSize = Math.max(1, Math.min(size, 50));
        PageRequest page = PageRequest.of(0, safeSize + 1);
        Flux<AssignmentEntity> source = cursor == null
                ? assignmentRepository.findAllByOrderByIdDesc(page)
                : assignmentRepository.findByIdLessThanOrderByIdDesc(cursor, page);
        return source.collectList().map(items -> {
            boolean hasNext = items.size() > safeSize;
            List<AssignmentResponse> pageItems = items.stream().limit(safeSize).map(this::toResponse).toList();
            Long nextCursor = hasNext && !pageItems.isEmpty() ? pageItems.get(pageItems.size() - 1).id() : null;
            return new CursorPage<>(pageItems, nextCursor, hasNext);
        });
    }

    public Mono<PagedAssignments> page(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 50));
        return Mono.zip(
                assignmentRepository.findAllByOrderByIdDesc(PageRequest.of(safePage, safeSize))
                        .map(this::toResponse).collectList(),
                assignmentRepository.count(),
                PagedAssignments::new);
    }

    public Mono<AssignmentResponse> get(Long id) {
        return requireAssignment(id).map(this::toResponse);
    }

    public Mono<AssignmentResponse> create(AssignmentCreateRequest request) {
        return Mono.fromCallable(() -> userClient.resolveAssignment(new AssignmentReferenceRequest(
                        request.teacherId(), request.classId(), request.subjectId())))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(reference -> assignmentRepository.save(new AssignmentEntity(
                        null, reference.teacherId(), reference.teacherUserId(), reference.teacherName(),
                        reference.classId(), reference.className(), reference.subjectId(), reference.subjectName(),
                        request.title(), request.description(), request.dueDate(), LocalDateTime.now())))
                .map(this::toResponse);
    }

    public Mono<AssignmentResponse> update(Long id, AssignmentUpdateRequest request) {
        return requireAssignment(id)
                .flatMap(assignment -> {
                    assignment.setTitle(request.title());
                    assignment.setDescription(request.description());
                    assignment.setDueDate(request.dueDate());
                    return assignmentRepository.save(assignment);
                })
                .map(this::toResponse);
    }

    public Mono<Void> delete(Long id) {
        return requireAssignment(id)
                .flatMap(assignment -> submissionRepository.deleteByAssignmentId(id)
                        .then(assignmentRepository.delete(assignment)))
                .as(transactionalOperator::transactional);
    }

    public Mono<SubmissionResponse> submit(Long assignmentId, SubmissionCreateRequest request) {
        return requireAssignment(assignmentId)
                .flatMap(assignment -> Mono.fromCallable(() -> userClient.resolveStudent(
                                new StudentReferenceRequest(request.studentId(), assignment.getClassId())))
                        .subscribeOn(Schedulers.boundedElastic())
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

    public Flux<SubmissionResponse> listSubmissions(Long assignmentId) {
        return requireAssignment(assignmentId)
                .thenMany(submissionRepository.findByAssignmentIdOrderByIdAsc(assignmentId))
                .map(this::toResponse);
    }

    public Mono<GradingContextResponse> gradingContext(Long assignmentId, Long studentId) {
        return Mono.zip(requireAssignment(assignmentId),
                        submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId)
                                .switchIfEmpty(Mono.error(new NotFoundException(
                                        "Решение ученика id=" + studentId + " по заданию id=" + assignmentId + " не найдено"))))
                .map(tuple -> {
                    AssignmentEntity assignment = tuple.getT1();
                    SubmissionEntity submission = tuple.getT2();
                    return new GradingContextResponse(
                            submission.getId(), assignment.getId(), submission.getStudentId(),
                            submission.getStudentName(), submission.getContentUrl(), submission.getSubmittedAt(),
                            assignment.getTeacherId(), assignment.getTeacherUserId(), assignment.getTeacherName(),
                            assignment.getTitle(), assignment.getSubjectName());
                });
    }

    private Mono<AssignmentEntity> requireAssignment(Long id) {
        return assignmentRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Домашнее задание id=" + id + " не найдено")));
    }

    private AssignmentResponse toResponse(AssignmentEntity entity) {
        return new AssignmentResponse(entity.getId(), entity.getTeacherId(), entity.getTeacherName(),
                entity.getClassId(), entity.getClassName(), entity.getSubjectId(), entity.getSubjectName(),
                entity.getTitle(), entity.getDescription(), entity.getDueDate(), entity.getCreatedAt());
    }

    private SubmissionResponse toResponse(SubmissionEntity entity) {
        return new SubmissionResponse(entity.getId(), entity.getAssignmentId(), entity.getStudentId(),
                entity.getStudentName(), entity.getContentUrl(), entity.getSubmittedAt(), null,
                entity.getStatus(), null);
    }

    public record PagedAssignments(List<AssignmentResponse> items, long total) {
    }
}
