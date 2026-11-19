package com.lab.grades.integration;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "user-service",
        contextId = "gradeScopeClient",
        path = "/internal/directory",
        fallback = GradeScopeClientFallback.class)
public interface GradeScopeClient {
    @GetMapping("/grade-scope/{userId}")
    GradeScopeResponse gradeScope(@PathVariable("userId") Long userId);
}
