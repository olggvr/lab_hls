package com.lab.school.controller;

import com.lab.school.dto.schoolclass.SchoolClassRequest;
import com.lab.school.dto.schoolclass.SchoolClassResponse;
import com.lab.school.dto.student.StudentResponse;
import com.lab.school.service.SchoolClassService;
import com.lab.school.service.StudentService;
import com.lab.school.utils.Locations;
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

import static com.lab.school.utils.PaginationConstants.DEFAULT_PAGE;
import static com.lab.school.utils.PaginationConstants.DEFAULT_PAGE_SIZE;
import static com.lab.school.utils.PaginationConstants.MAX_PAGE_SIZE;

@Tag(name = "Classes", description = "Классы")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/classes")
public class SchoolClassController {

    private final SchoolClassService schoolClassService;
    private final StudentService studentService;

    @Operation(summary = "Список классов (постранично, не более 50)")
    @GetMapping
    public ResponseEntity<List<SchoolClassResponse>> listClasses(
            @RequestParam(defaultValue = DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return ResponseEntity.ok(schoolClassService.listClasses(page, size).getContent());
    }

    @Operation(summary = "Класс по id")
    @GetMapping("/{id}")
    public ResponseEntity<SchoolClassResponse> getSchoolClass(@PathVariable Long id) {
        return ResponseEntity.ok(schoolClassService.getSchoolClass(id));
    }

    @Operation(summary = "Ученики класса (One-to-Many classes -> students)")
    @GetMapping("/{id}/students")
    public ResponseEntity<List<StudentResponse>> listStudentsOfClass(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.listStudentsOfClass(id));
    }

    @Operation(summary = "Создать класс")
    @PostMapping
    public ResponseEntity<SchoolClassResponse> createSchoolClass(@Valid @RequestBody SchoolClassRequest request) {
        SchoolClassResponse created = schoolClassService.createSchoolClass(request);
        return ResponseEntity.created(Locations.buildFor(created.id())).body(created);
    }

    @Operation(summary = "Обновить класс")
    @PutMapping("/{id}")
    public ResponseEntity<SchoolClassResponse> updateSchoolClass(@PathVariable Long id,
                                                                 @Valid @RequestBody SchoolClassRequest request) {
        return ResponseEntity.ok(schoolClassService.updateSchoolClass(id, request));
    }

    @Operation(summary = "Удалить класс")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchoolClass(@PathVariable Long id) {
        schoolClassService.deleteSchoolClass(id);
        return ResponseEntity.noContent().build();
    }
}
