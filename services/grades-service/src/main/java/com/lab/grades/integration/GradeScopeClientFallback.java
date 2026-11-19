package com.lab.grades.integration;

import com.lab.grades.exception.DownstreamUnavailableException;
import org.springframework.stereotype.Component;

@Component
public class GradeScopeClientFallback implements GradeScopeClient {
    @Override
    public GradeScopeResponse gradeScope(Long userId) {
        throw new DownstreamUnavailableException("User Service временно недоступен");
    }
}
