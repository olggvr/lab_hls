package com.lab.school.controller;

import com.lab.school.dto.assignment.AssignmentCreateRequest;
import com.lab.school.dto.assignment.AssignmentResponse;
import com.lab.school.dto.assignment.AssignmentUpdateRequest;
import com.lab.school.dto.assignment.GradeRequest;
import com.lab.school.dto.assignment.GradeResponse;
import com.lab.school.dto.assignment.SubmissionCreateRequest;
import com.lab.school.dto.assignment.SubmissionResponse;
import com.lab.school.dto.common.CursorPage;
import com.lab.school.service.AssignmentService;
import com.lab.school.service.GradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
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

import java.util.List;

import static com.lab.school.dto.common.PaginationConstants.DEFAULT_PAGE;
import static com.lab.school.dto.common.PaginationConstants.DEFAULT_PAGE_SIZE;
import static com.lab.school.dto.common.PaginationConstants.MAX_PAGE_SIZE;
import static com.lab.school.dto.common.PaginationConstants.TOTAL_COUNT_HEADER;

@Tag(name = "Assignments", description = "Домашние задания, решения и оценки")
@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final GradeService gradeService;

    public AssignmentController(AssignmentService assignmentService, GradeService gradeService) {
        this.assignmentService = assignmentService;
        this.gradeService = gradeService;
    }

    @Operation(summary = "Лента ДЗ для infinite scroll (без общего количества)",
            description = "Первый запрос — без cursor. Следующие — с cursor = nextCursor из предыдущего ответа.")
    @GetMapping
    public CursorPage<AssignmentResponse> scroll(
            @Parameter(description = "id последнего полученного задания")
            @RequestParam(required = false) @Positive Long cursor,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return assignmentService.scroll(cursor, size);
    }

    @Operation(summary = "Список ДЗ постранично с общим количеством в заголовке X-Total-Count",
            responses = @ApiResponse(responseCode = "200", headers = @Header(name = TOTAL_COUNT_HEADER,
                    description = "Общее количество заданий")))
    @GetMapping("/page")
    public ResponseEntity<List<AssignmentResponse>> page(
            @RequestParam(defaultValue = DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        Page<AssignmentResponse> result = assignmentService.findPage(page, size);
        return ResponseEntity.ok()
                .header(TOTAL_COUNT_HEADER, String.valueOf(result.getTotalElements()))
                .body(result.getContent());
    }

    @Operation(summary = "ДЗ по id")
    @GetMapping("/{id}")
    public AssignmentResponse getById(@PathVariable Long id) {
        return assignmentService.getById(id);
    }

    @Operation(summary = "Создать ДЗ (учитель должен вести этот предмет в этом классе)")
    @PostMapping
    public ResponseEntity<AssignmentResponse> create(@Valid @RequestBody AssignmentCreateRequest request) {
        AssignmentResponse created = assignmentService.create(request);
        return ResponseEntity.created(Locations.of(created.id())).body(created);
    }

    @Operation(summary = "Изменить ДЗ")
    @PutMapping("/{id}")
    public AssignmentResponse update(@PathVariable Long id, @Valid @RequestBody AssignmentUpdateRequest request) {
        return assignmentService.update(id, request);
    }

    @Operation(summary = "Удалить ДЗ (вместе с решениями)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        assignmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Отправить решение ДЗ")
    @PostMapping("/{id}/submissions")
    public ResponseEntity<SubmissionResponse> submit(@PathVariable Long id,
                                                     @Valid @RequestBody SubmissionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assignmentService.submit(id, request));
    }

    @Operation(summary = "Решения по ДЗ (One-to-Many assignments -> submissions)")
    @GetMapping("/{id}/submissions")
    public List<SubmissionResponse> submissions(@PathVariable Long id) {
        return assignmentService.getSubmissions(id);
    }

    @Operation(summary = "Выставить оценку и уведомить ученика и родителей (одна транзакция)")
    @PostMapping("/{id}/grade")
    public GradeResponse grade(@PathVariable Long id, @Valid @RequestBody GradeRequest request) {
        return gradeService.grade(id, request);
    }
}
