package com.lab.school.controller;

import com.lab.school.dto.parent.ParentContactResponse;
import com.lab.school.dto.parent.ParentLinkRequest;
import com.lab.school.dto.student.StudentCreateRequest;
import com.lab.school.dto.student.StudentResponse;
import com.lab.school.dto.student.StudentUpdateRequest;
import com.lab.school.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

import static com.lab.school.dto.common.PaginationConstants.DEFAULT_PAGE;
import static com.lab.school.dto.common.PaginationConstants.DEFAULT_PAGE_SIZE;
import static com.lab.school.dto.common.PaginationConstants.MAX_PAGE_SIZE;
import static com.lab.school.dto.common.PaginationConstants.TOTAL_COUNT_HEADER;

@Tag(name = "Students", description = "Ученики и связь с родителями")
@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @Operation(summary = "Список учеников (постранично, не более 50)")
    @GetMapping
    public List<StudentResponse> findAll(
            @RequestParam(defaultValue = DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return studentService.findAll(page, size).getContent();
    }

    @Operation(summary = "Список учеников с общим количеством в заголовке X-Total-Count",
            responses = @ApiResponse(responseCode = "200", headers = @Header(name = TOTAL_COUNT_HEADER,
                    description = "Общее количество учеников")))
    @GetMapping("/page")
    public ResponseEntity<List<StudentResponse>> page(
            @RequestParam(defaultValue = DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        Page<StudentResponse> result = studentService.findAll(page, size);
        return ResponseEntity.ok()
                .header(TOTAL_COUNT_HEADER, String.valueOf(result.getTotalElements()))
                .body(result.getContent());
    }

    @Operation(summary = "Ученик по id")
    @GetMapping("/{id}")
    public StudentResponse getById(@PathVariable Long id) {
        return studentService.getById(id);
    }

    @Operation(summary = "Создать профиль ученика для пользователя с ролью STUDENT")
    @PostMapping
    public ResponseEntity<StudentResponse> create(@Valid @RequestBody StudentCreateRequest request) {
        StudentResponse created = studentService.create(request);
        return ResponseEntity.created(Locations.of(created.id())).body(created);
    }

    @Operation(summary = "Перевести ученика в другой класс")
    @PutMapping("/{id}")
    public StudentResponse update(@PathVariable Long id, @Valid @RequestBody StudentUpdateRequest request) {
        return studentService.update(id, request);
    }

    @Operation(summary = "Удалить профиль ученика")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        studentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Контакты родителей ученика")
    @GetMapping("/{id}/parents")
    public List<ParentContactResponse> parents(@PathVariable Long id) {
        return studentService.getParents(id);
    }

    @Operation(summary = "Привязать родителя к ученику (M:N с доп. полем relation)")
    @PostMapping("/{id}/parents")
    public ResponseEntity<ParentContactResponse> linkParent(@PathVariable Long id,
                                                            @Valid @RequestBody ParentLinkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentService.linkParent(id, request));
    }

    @Operation(summary = "Отвязать родителя от ученика")
    @DeleteMapping("/{id}/parents/{parentId}")
    public ResponseEntity<Void> unlinkParent(@PathVariable Long id, @PathVariable Long parentId) {
        studentService.unlinkParent(id, parentId);
        return ResponseEntity.noContent().build();
    }
}
