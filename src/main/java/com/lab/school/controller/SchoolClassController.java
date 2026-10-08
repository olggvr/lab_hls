package com.lab.school.controller;

import com.lab.school.dto.schoolclass.SchoolClassRequest;
import com.lab.school.dto.schoolclass.SchoolClassResponse;
import com.lab.school.dto.student.StudentResponse;
import com.lab.school.service.SchoolClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

@Tag(name = "Classes", description = "Классы")
@RestController
@RequestMapping("/api/classes")
public class SchoolClassController {

    private final SchoolClassService schoolClassService;

    public SchoolClassController(SchoolClassService schoolClassService) {
        this.schoolClassService = schoolClassService;
    }

    @Operation(summary = "Список классов (постранично, не более 50)")
    @GetMapping
    public List<SchoolClassResponse> findAll(
            @RequestParam(defaultValue = DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return schoolClassService.findAll(page, size).getContent();
    }

    @Operation(summary = "Класс по id")
    @GetMapping("/{id}")
    public SchoolClassResponse getById(@PathVariable Long id) {
        return schoolClassService.getById(id);
    }

    @Operation(summary = "Ученики класса (One-to-Many classes -> students)")
    @GetMapping("/{id}/students")
    public List<StudentResponse> students(@PathVariable Long id) {
        return schoolClassService.getStudents(id);
    }

    @Operation(summary = "Создать класс")
    @PostMapping
    public ResponseEntity<SchoolClassResponse> create(@Valid @RequestBody SchoolClassRequest request) {
        SchoolClassResponse created = schoolClassService.create(request);
        return ResponseEntity.created(Locations.of(created.id())).body(created);
    }

    @Operation(summary = "Обновить класс")
    @PutMapping("/{id}")
    public SchoolClassResponse update(@PathVariable Long id, @Valid @RequestBody SchoolClassRequest request) {
        return schoolClassService.update(id, request);
    }

    @Operation(summary = "Удалить класс")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        schoolClassService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
