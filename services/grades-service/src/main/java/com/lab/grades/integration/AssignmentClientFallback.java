package com.lab.grades.integration;

import com.lab.grades.exception.DownstreamUnavailableException;
import org.springframework.stereotype.Component;

@Component
public class AssignmentClientFallback implements AssignmentClient {
    @Override
    public GradingContextResponse gradingContext(Long assignmentId, Long studentId) {
        throw new DownstreamUnavailableException("Assignment Service временно недоступен");
    }
}
