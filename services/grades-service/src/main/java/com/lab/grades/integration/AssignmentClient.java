package com.lab.grades.integration;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "assignment-service", path = "/internal/assignments", fallback = AssignmentClientFallback.class)
public interface AssignmentClient {
    @GetMapping("/{assignmentId}/submissions/by-student/{studentId}")
    GradingContextResponse gradingContext(
            @PathVariable("assignmentId") Long assignmentId,
            @PathVariable("studentId") Long studentId);
}
