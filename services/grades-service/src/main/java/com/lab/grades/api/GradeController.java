package com.lab.grades.api;

import com.lab.grades.service.ReactiveGradeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@Validated
@RequiredArgsConstructor
public class GradeController {
    private final ReactiveGradeService gradeService;

    @PostMapping("/api/assignments/{assignmentId}/grade")
    public Mono<GradeResponse> grade(
            @PathVariable @Positive Long assignmentId, @Valid @RequestBody GradeRequest request) {
        return gradeService.grade(assignmentId, request);
    }

    @GetMapping("/api/users/{userId}/grades")
    public Flux<GradeEntryResponse> grades(@PathVariable @Positive Long userId) {
        return gradeService.listForUser(userId);
    }
}
