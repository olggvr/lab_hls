package com.lab.assignment.api;

import com.lab.assignment.service.SubmissionService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/internal/assignments")
public class InternalAssignmentController {
    private final SubmissionService submissionService;

    @GetMapping("/{assignmentId}/submissions/by-student/{studentId}")
    public Mono<GradingContextResponse> gradingContext(
            @PathVariable @Positive Long assignmentId, @PathVariable @Positive Long studentId) {
        return submissionService.gradingContext(assignmentId, studentId);
    }
}
