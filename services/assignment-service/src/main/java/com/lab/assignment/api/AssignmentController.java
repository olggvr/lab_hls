package com.lab.assignment.api;

import com.lab.assignment.service.ReactiveAssignmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/assignments")
public class AssignmentController {
    private final ReactiveAssignmentService assignmentService;

    @GetMapping
    public Mono<ResponseEntity<CursorPage<AssignmentResponse>>> scroll(
            @RequestParam(required = false) @Positive Long cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return assignmentService.scroll(cursor, size).map(ResponseEntity::ok);
    }

    @GetMapping("/page")
    public Mono<ResponseEntity<List<AssignmentResponse>>> page(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return assignmentService.page(page, size).map(result -> ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.total())).body(result.items()));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<AssignmentResponse>> get(@PathVariable Long id) {
        return assignmentService.get(id).map(ResponseEntity::ok);
    }

    @PostMapping
    public Mono<ResponseEntity<AssignmentResponse>> create(@Valid @RequestBody AssignmentCreateRequest request) {
        return assignmentService.create(request)
                .map(body -> ResponseEntity.created(URI.create("/api/assignments/" + body.id())).body(body));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<AssignmentResponse>> update(
            @PathVariable Long id, @Valid @RequestBody AssignmentUpdateRequest request) {
        return assignmentService.update(id, request).map(ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable Long id) {
        return assignmentService.delete(id).thenReturn(ResponseEntity.noContent().build());
    }

    @PostMapping("/{id}/submissions")
    public Mono<ResponseEntity<SubmissionResponse>> submit(
            @PathVariable Long id, @Valid @RequestBody SubmissionCreateRequest request) {
        return assignmentService.submit(id, request)
                .map(body -> ResponseEntity.created(URI.create("/api/assignments/" + id + "/submissions/" + body.id()))
                        .body(body));
    }

    @GetMapping("/{id}/submissions")
    public Flux<SubmissionResponse> submissions(@PathVariable Long id) {
        return assignmentService.listSubmissions(id);
    }
}
