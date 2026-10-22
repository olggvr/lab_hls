package com.lab.assignment.integration;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service", path = "/internal/directory", fallback = UserClientFallback.class)
public interface UserClient {
    @PostMapping("/assignment")
    AssignmentReferenceResponse resolveAssignment(@RequestBody AssignmentReferenceRequest request);

    @PostMapping("/student")
    StudentReferenceResponse resolveStudent(@RequestBody StudentReferenceRequest request);
}
