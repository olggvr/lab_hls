package com.lab.school.controller;

import com.lab.school.dto.subject.SubjectRequest;
import com.lab.school.dto.subject.SubjectResponse;
import com.lab.school.service.SubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

import java.util.List;

import static com.lab.school.dto.common.PaginationConstants.DEFAULT_PAGE;
import static com.lab.school.dto.common.PaginationConstants.DEFAULT_PAGE_SIZE;
import static com.lab.school.dto.common.PaginationConstants.MAX_PAGE_SIZE;

@Tag(name = "Subjects", description = "Учебные предметы")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subjects")
public class SubjectController {

    private final SubjectService subjectService;

    @Operation(summary = "Список предметов (постранично, не более 50)")
    @GetMapping
    public ResponseEntity<List<SubjectResponse>> listSubjects(
            @RequestParam(defaultValue = DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return ResponseEntity.ok(subjectService.listSubjects(page, size).getContent());
    }

    @Operation(summary = "Предмет по id")
    @GetMapping("/{id}")
    public ResponseEntity<SubjectResponse> getSubject(@PathVariable Long id) {
        return ResponseEntity.ok(subjectService.getSubject(id));
    }

    @Operation(summary = "Создать предмет")
    @PostMapping
    public ResponseEntity<SubjectResponse> createSubject(@Valid @RequestBody SubjectRequest request) {
        SubjectResponse created = subjectService.createSubject(request);
        return ResponseEntity.created(Locations.of(created.id())).body(created);
    }

    @Operation(summary = "Переименовать предмет")
    @PutMapping("/{id}")
    public ResponseEntity<SubjectResponse> updateSubject(@PathVariable Long id,
                                                         @Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.ok(subjectService.updateSubject(id, request));
    }

    @Operation(summary = "Удалить предмет")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubject(@PathVariable Long id) {
        subjectService.deleteSubject(id);
        return ResponseEntity.noContent().build();
    }
}
