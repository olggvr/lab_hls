package com.lab.assignment.api;

import com.lab.assignment.service.ReactiveAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/assignments")
public class InternalAssignmentController {
    private final ReactiveAssignmentService assignmentService;

    @GetMapping("/{assignmentId}/submissions/by-student/{studentId}")
    public Mono<GradingContextResponse> gradingContext(
            @PathVariable Long assignmentId, @PathVariable Long studentId) {
        return assignmentService.gradingContext(assignmentId, studentId);
    }
}
