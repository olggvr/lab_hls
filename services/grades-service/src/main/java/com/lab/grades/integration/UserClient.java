package com.lab.grades.integration;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", path = "/internal/directory", fallback = UserClientFallback.class)
public interface UserClient {
    @GetMapping("/grade-audience/{studentId}")
    GradeAudienceResponse gradeAudience(@PathVariable("studentId") Long studentId);

    @GetMapping("/grade-scope/{userId}")
    GradeScopeResponse gradeScope(@PathVariable("userId") Long userId);
}
