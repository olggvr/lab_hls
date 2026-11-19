package com.lab.grades.integration;

import com.lab.grades.exception.DownstreamUnavailableException;
import org.springframework.stereotype.Component;

@Component
public class GradeAudienceClientFallback implements GradeAudienceClient {
    @Override
    public GradeAudienceResponse gradeAudience(Long studentId) {
        throw new DownstreamUnavailableException("User Service временно недоступен");
    }
}
