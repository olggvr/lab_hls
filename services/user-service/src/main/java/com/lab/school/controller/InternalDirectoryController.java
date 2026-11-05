package com.lab.school.controller;

import com.lab.school.integration.AudienceResponse;
import com.lab.school.integration.AssignmentReferenceRequest;
import com.lab.school.integration.AssignmentReferenceResponse;
import com.lab.school.integration.BroadcastAudienceRequest;
import com.lab.school.integration.DirectAudienceRequest;
import com.lab.school.integration.GradeAudienceResponse;
import com.lab.school.integration.GradeScopeResponse;
import com.lab.school.integration.StudentReferenceRequest;
import com.lab.school.integration.StudentReferenceResponse;
import com.lab.school.service.ReactiveDirectoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/internal/directory")
public class InternalDirectoryController {
    private final ReactiveDirectoryService directoryService;

    @PostMapping("/direct")
    public Mono<AudienceResponse> resolveDirect(@Valid @RequestBody DirectAudienceRequest request) {
        return directoryService.resolveDirect(request);
    }

    @PostMapping("/broadcast")
    public Mono<AudienceResponse> resolveBroadcast(@Valid @RequestBody BroadcastAudienceRequest request) {
        return directoryService.resolveBroadcast(request);
    }

    @PostMapping("/assignment")
    public Mono<AssignmentReferenceResponse> resolveAssignment(
            @Valid @RequestBody AssignmentReferenceRequest request) {
        return directoryService.resolveAssignment(request);
    }

    @PostMapping("/student")
    public Mono<StudentReferenceResponse> resolveStudent(@Valid @RequestBody StudentReferenceRequest request) {
        return directoryService.resolveStudent(request);
    }

    @GetMapping("/grade-audience/{studentId}")
    public Mono<GradeAudienceResponse> resolveGradeAudience(@PathVariable @Positive Long studentId) {
        return directoryService.resolveGradeAudience(studentId);
    }

    @GetMapping("/grade-scope/{userId}")
    public Mono<GradeScopeResponse> resolveGradeScope(@PathVariable @Positive Long userId) {
        return directoryService.resolveGradeScope(userId);
    }
}
