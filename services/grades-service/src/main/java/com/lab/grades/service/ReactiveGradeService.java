package com.lab.grades.service;

import com.lab.grades.api.GradeEntryResponse;
import com.lab.grades.api.GradeRequest;
import com.lab.grades.api.GradeResponse;
import com.lab.grades.api.GradedSubmissionResponse;
import com.lab.grades.domain.GradeEntity;
import com.lab.grades.integration.AssignmentClient;
import com.lab.grades.integration.GradeAudienceResponse;
import com.lab.grades.integration.GradeNotificationRequest;
import com.lab.grades.integration.GradeScopeResponse;
import com.lab.grades.integration.GradingContextResponse;
import com.lab.grades.integration.MessageClient;
import com.lab.grades.integration.NotificationResponse;
import com.lab.grades.integration.UserClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class ReactiveGradeService {
    private final AssignmentClient assignmentClient;
    private final UserClient userClient;
    private final MessageClient messageClient;
    private final GradePersistenceService persistenceService;

    public Mono<GradeResponse> grade(Long assignmentId, GradeRequest request) {
        return assignmentClient.gradingContext(assignmentId, request.studentId())
                .zipWith(Mono.fromCallable(() -> userClient.gradeAudience(request.studentId()))
                                .subscribeOn(Schedulers.boundedElastic()),
                        GradeContext::new)
                .flatMap(context -> Mono.fromCallable(() -> persistenceService.save(context.assignment(), request.grade()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(saved -> new PersistedGrade(context, saved)))
                .flatMap(result -> messageClient.sendGradeNotification(
                                notification(result.context().assignment(), result.context().audience(), request.grade()))
                        .map(notification -> toResponse(result.grade(), notification)));
    }

    public Flux<GradeEntryResponse> listForUser(Long userId) {
        return Mono.fromCallable(() -> userClient.gradeScope(userId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(scope -> Flux.defer(() -> Flux.fromIterable(persistenceService.findForScope(scope)))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(this::toEntry);
    }

    private GradeNotificationRequest notification(
            GradingContextResponse assignment, GradeAudienceResponse audience, int value) {
        return new GradeNotificationRequest(
                assignment.teacherUserId(), assignment.teacherName(),
                "Новая оценка: " + assignment.subjectName(),
                "Ученик " + audience.studentName() + " получил оценку " + value
                        + " за задание «" + assignment.assignmentTitle() + "».",
                audience.recipientIds());
    }

    private GradeResponse toResponse(GradeEntity grade, NotificationResponse notification) {
        GradedSubmissionResponse submission = new GradedSubmissionResponse(
                grade.getSubmissionId(), grade.getAssignmentId(), grade.getStudentId(), grade.getStudentName(),
                grade.getContentUrl(), grade.getSubmittedAt(), grade.getGrade(), "GRADED", grade.getGradedAt());
        return new GradeResponse(submission, notification.messageId(), notification.recipientsCount());
    }

    private GradeEntryResponse toEntry(GradeEntity grade) {
        return new GradeEntryResponse(grade.getSubmissionId(), grade.getAssignmentId(), grade.getAssignmentTitle(),
                grade.getSubjectName(), grade.getStudentId(), grade.getStudentName(), grade.getGrade(),
                grade.getGradedAt());
    }

    private record GradeContext(GradingContextResponse assignment, GradeAudienceResponse audience) {
    }

    private record PersistedGrade(GradeContext context, GradeEntity grade) {
    }
}
