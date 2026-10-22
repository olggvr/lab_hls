package com.lab.grades.api;

import com.lab.grades.service.ReactiveGradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class GradeController {
    private final ReactiveGradeService gradeService;

    @PostMapping("/api/assignments/{assignmentId}/grade")
    public Mono<ResponseEntity<GradeResponse>> grade(
            @PathVariable Long assignmentId, @Valid @RequestBody GradeRequest request) {
        return gradeService.grade(assignmentId, request).map(ResponseEntity::ok);
    }

    @GetMapping("/api/users/{userId}/grades")
    public Flux<GradeEntryResponse> grades(@PathVariable Long userId) {
        return gradeService.listForUser(userId);
    }
}
