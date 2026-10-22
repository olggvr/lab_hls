package com.lab.assignment.integration;

import com.lab.assignment.exception.DownstreamUnavailableException;
import org.springframework.stereotype.Component;

@Component
public class UserClientFallback implements UserClient {
    @Override
    public AssignmentReferenceResponse resolveAssignment(AssignmentReferenceRequest request) {
        throw unavailable();
    }

    @Override
    public StudentReferenceResponse resolveStudent(StudentReferenceRequest request) {
        throw unavailable();
    }

    private DownstreamUnavailableException unavailable() {
        return new DownstreamUnavailableException("User Service временно недоступен");
    }
}
