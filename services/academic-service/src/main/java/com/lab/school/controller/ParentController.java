package com.lab.school.controller;

import com.lab.school.dto.parent.ChildResponse;
import com.lab.school.dto.parent.ParentCreateRequest;
import com.lab.school.dto.parent.ParentResponse;
import com.lab.school.service.ParentService;
import com.lab.school.service.ParentStudentService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.lab.school.dto.common.PaginationConstants.DEFAULT_PAGE;
import static com.lab.school.dto.common.PaginationConstants.DEFAULT_PAGE_SIZE;
import static com.lab.school.dto.common.PaginationConstants.MAX_PAGE_SIZE;

@Tag(name = "Parents", description = "Родители")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/parents")
public class ParentController {

    private final ParentService parentService;
    private final ParentStudentService parentStudentService;

    @Operation(summary = "Список родителей (постранично, не более 50)")
    @GetMapping
    public ResponseEntity<List<ParentResponse>> listParents(
            @RequestParam(defaultValue = DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return ResponseEntity.ok(parentService.listParents(page, size).getContent());
    }

    @Operation(summary = "Родитель по id")
    @GetMapping("/{id}")
    public ResponseEntity<ParentResponse> getParent(@PathVariable Long id) {
        return ResponseEntity.ok(parentService.getParent(id));
    }

    @Operation(summary = "Создать профиль родителя для пользователя с ролью PARENT")
    @PostMapping
    public ResponseEntity<ParentResponse> createParent(@Valid @RequestBody ParentCreateRequest request) {
        ParentResponse created = parentService.createParent(request);
        return ResponseEntity.created(Locations.of(created.id())).body(created);
    }

    @Operation(summary = "Удалить профиль родителя")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteParent(@PathVariable Long id) {
        parentService.deleteParent(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Дети родителя")
    @GetMapping("/{id}/children")
    public ResponseEntity<List<ChildResponse>> listChildrenOfParent(@PathVariable Long id) {
        return ResponseEntity.ok(parentStudentService.listChildrenOfParent(id));
    }
}
