package com.lab.grades.integration;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "user-service",
        contextId = "gradeAudienceClient",
        path = "/internal/directory",
        fallback = GradeAudienceClientFallback.class)
public interface GradeAudienceClient {
    @GetMapping("/grade-audience/{studentId}")
    GradeAudienceResponse gradeAudience(@PathVariable("studentId") Long studentId);
}
