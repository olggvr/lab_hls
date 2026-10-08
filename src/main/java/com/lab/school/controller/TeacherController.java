package com.lab.school.controller;

import com.lab.school.dto.teacher.TeacherCreateRequest;
import com.lab.school.dto.teacher.TeacherResponse;
import com.lab.school.dto.teacher.TeacherUpdateRequest;
import com.lab.school.dto.teacher.WorkloadRequest;
import com.lab.school.dto.teacher.WorkloadResponse;
import com.lab.school.service.TeacherService;
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

@Tag(name = "Teachers", description = "Учителя и их нагрузка")
@RestController
@RequestMapping("/api/teachers")
public class TeacherController {

    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @Operation(summary = "Список учителей (постранично, не более 50)")
    @GetMapping
    public List<TeacherResponse> findAll(
            @RequestParam(defaultValue = DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return teacherService.findAll(page, size).getContent();
    }

    @Operation(summary = "Учитель по id")
    @GetMapping("/{id}")
    public TeacherResponse getById(@PathVariable Long id) {
        return teacherService.getById(id);
    }

    @Operation(summary = "Создать профиль учителя для пользователя с ролью TEACHER")
    @PostMapping
    public ResponseEntity<TeacherResponse> create(@Valid @RequestBody TeacherCreateRequest request) {
        TeacherResponse created = teacherService.create(request);
        return ResponseEntity.created(Locations.of(created.id())).body(created);
    }

    @Operation(summary = "Сменить основной предмет учителя")
    @PutMapping("/{id}")
    public TeacherResponse update(@PathVariable Long id, @Valid @RequestBody TeacherUpdateRequest request) {
        return teacherService.update(id, request);
    }

    @Operation(summary = "Удалить профиль учителя")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        teacherService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Нагрузка учителя: классы и предметы по учебным годам")
    @GetMapping("/{id}/workload")
    public List<WorkloadResponse> workload(@PathVariable Long id) {
        return teacherService.getWorkload(id);
    }

    @Operation(summary = "Назначить учителя на класс и предмет (M:N с доп. полем academic_year)")
    @PostMapping("/{id}/workload")
    public ResponseEntity<WorkloadResponse> addWorkload(@PathVariable Long id,
                                                        @Valid @RequestBody WorkloadRequest request) {
        WorkloadResponse created = teacherService.addWorkload(id, request);
        return ResponseEntity.created(Locations.of(created.id())).body(created);
    }

    @Operation(summary = "Снять назначение")
    @DeleteMapping("/{id}/workload/{workloadId}")
    public ResponseEntity<Void> removeWorkload(@PathVariable Long id, @PathVariable Long workloadId) {
        teacherService.removeWorkload(id, workloadId);
        return ResponseEntity.noContent().build();
    }
}
