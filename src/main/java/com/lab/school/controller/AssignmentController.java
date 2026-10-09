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
import com.lab.school.utils.Locations;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
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

import static com.lab.school.utils.PaginationConstants.DEFAULT_PAGE;
import static com.lab.school.utils.PaginationConstants.DEFAULT_PAGE_SIZE;
import static com.lab.school.utils.PaginationConstants.MAX_PAGE_SIZE;
import static com.lab.school.utils.PaginationConstants.TOTAL_COUNT_HEADER;

@Tag(name = "Assignments", description = "Домашние задания, решения и оценки")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final GradeService gradeService;

    @Operation(summary = "Лента ДЗ для infinite scroll (без общего количества)",
            description = "Первый запрос — без cursor. Следующие — с cursor = nextCursor из предыдущего ответа.")
    @GetMapping
    public ResponseEntity<CursorPage<AssignmentResponse>> scrollAssignments(
            @Parameter(description = "id последнего полученного задания")
            @RequestParam(required = false) @Positive Long cursor,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return ResponseEntity.ok(assignmentService.scrollAssignments(cursor, size));
    }

    @Operation(summary = "Список ДЗ постранично с общим количеством в заголовке X-Total-Count",
            responses = @ApiResponse(responseCode = "200", headers = @Header(name = TOTAL_COUNT_HEADER,
                    description = "Общее количество заданий")))
    @GetMapping("/page")
    public ResponseEntity<List<AssignmentResponse>> listAssignmentsPage(
            @RequestParam(defaultValue = DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        Page<AssignmentResponse> result = assignmentService.listAssignmentsPage(page, size);
        return ResponseEntity.ok()
                .header(TOTAL_COUNT_HEADER, String.valueOf(result.getTotalElements()))
                .body(result.getContent());
    }

    @Operation(summary = "ДЗ по id")
    @GetMapping("/{id}")
    public ResponseEntity<AssignmentResponse> getAssignment(@PathVariable Long id) {
        return ResponseEntity.ok(assignmentService.getAssignment(id));
    }

    @Operation(summary = "Создать ДЗ (учитель должен вести этот предмет в этом классе)")
    @PostMapping
    public ResponseEntity<AssignmentResponse> createAssignment(@Valid @RequestBody AssignmentCreateRequest request) {
        AssignmentResponse created = assignmentService.createAssignment(request);
        return ResponseEntity.created(Locations.buildFor(created.id())).body(created);
    }

    @Operation(summary = "Изменить ДЗ")
    @PutMapping("/{id}")
    public ResponseEntity<AssignmentResponse> updateAssignment(@PathVariable Long id,
                                                               @Valid @RequestBody AssignmentUpdateRequest request) {
        return ResponseEntity.ok(assignmentService.updateAssignment(id, request));
    }

    @Operation(summary = "Удалить ДЗ (вместе с решениями)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAssignment(@PathVariable Long id) {
        assignmentService.deleteAssignment(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Отправить решение ДЗ")
    @PostMapping("/{id}/submissions")
    public ResponseEntity<SubmissionResponse> submitSolution(@PathVariable Long id,
                                                             @Valid @RequestBody SubmissionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assignmentService.submitSolution(id, request));
    }

    @Operation(summary = "Решения по ДЗ (One-to-Many assignments -> submissions)")
    @GetMapping("/{id}/submissions")
    public ResponseEntity<List<SubmissionResponse>> listSubmissions(@PathVariable Long id) {
        return ResponseEntity.ok(assignmentService.listSubmissions(id));
    }

    @Operation(summary = "Выставить оценку и уведомить ученика и родителей (одна транзакция)")
    @PostMapping("/{id}/grade")
    public ResponseEntity<GradeResponse> gradeSubmission(@PathVariable Long id,
                                                         @Valid @RequestBody GradeRequest request) {
        return ResponseEntity.ok(gradeService.gradeSubmission(id, request));
    }
}
