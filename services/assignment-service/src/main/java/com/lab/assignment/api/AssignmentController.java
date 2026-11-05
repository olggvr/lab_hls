package com.lab.assignment.api;

import com.lab.assignment.service.AssignmentService;
import com.lab.assignment.service.SubmissionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/assignments")
public class AssignmentController {
    private final AssignmentService assignmentService;
    private final SubmissionService submissionService;

    @GetMapping
    public Mono<CursorPage<AssignmentResponse>> scroll(
            @RequestParam(required = false) @Positive Long cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return assignmentService.scroll(cursor, size);
    }

    @GetMapping("/page")
    public Flux<AssignmentResponse> page(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            ServerHttpResponse response) {
        return assignmentService.count()
                .doOnNext(total -> response.getHeaders().set("X-Total-Count", String.valueOf(total)))
                .thenMany(assignmentService.page(page, size));
    }

    @GetMapping("/{id}")
    public Mono<AssignmentResponse> get(@PathVariable @Positive Long id) {
        return assignmentService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<AssignmentResponse> create(@Valid @RequestBody AssignmentCreateRequest request) {
        return assignmentService.create(request);
    }

    @PutMapping("/{id}")
    public Mono<AssignmentResponse> update(
            @PathVariable @Positive Long id, @Valid @RequestBody AssignmentUpdateRequest request) {
        return assignmentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@PathVariable @Positive Long id) {
        return assignmentService.delete(id);
    }

    @PostMapping("/{id}/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<SubmissionResponse> submit(
            @PathVariable @Positive Long id, @Valid @RequestBody SubmissionCreateRequest request) {
        return submissionService.submit(id, request);
    }

    @GetMapping("/{id}/submissions")
    public Flux<SubmissionResponse> submissions(@PathVariable @Positive Long id) {
        return submissionService.list(id);
    }
}
