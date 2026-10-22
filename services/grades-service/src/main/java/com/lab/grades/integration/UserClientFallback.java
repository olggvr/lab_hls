package com.lab.grades.integration;

import com.lab.grades.exception.DownstreamUnavailableException;
import org.springframework.stereotype.Component;

@Component
public class UserClientFallback implements UserClient {
    @Override
    public GradeAudienceResponse gradeAudience(Long studentId) {
        throw unavailable();
    }

    @Override
    public GradeScopeResponse gradeScope(Long userId) {
        throw unavailable();
    }

    private DownstreamUnavailableException unavailable() {
        return new DownstreamUnavailableException("User Service временно недоступен");
    }
}
