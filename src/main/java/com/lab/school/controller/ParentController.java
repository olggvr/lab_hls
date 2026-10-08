package com.lab.school.controller;

import com.lab.school.dto.parent.ChildResponse;
import com.lab.school.dto.parent.ParentCreateRequest;
import com.lab.school.dto.parent.ParentResponse;
import com.lab.school.service.ParentService;
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
@RequestMapping("/api/parents")
public class ParentController {

    private final ParentService parentService;

    public ParentController(ParentService parentService) {
        this.parentService = parentService;
    }

    @Operation(summary = "Список родителей (постранично, не более 50)")
    @GetMapping
    public List<ParentResponse> findAll(
            @RequestParam(defaultValue = DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return parentService.findAll(page, size).getContent();
    }

    @Operation(summary = "Родитель по id")
    @GetMapping("/{id}")
    public ParentResponse getById(@PathVariable Long id) {
        return parentService.getById(id);
    }

    @Operation(summary = "Создать профиль родителя для пользователя с ролью PARENT")
    @PostMapping
    public ResponseEntity<ParentResponse> create(@Valid @RequestBody ParentCreateRequest request) {
        ParentResponse created = parentService.create(request);
        return ResponseEntity.created(Locations.of(created.id())).body(created);
    }

    @Operation(summary = "Удалить профиль родителя")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        parentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Дети родителя")
    @GetMapping("/{id}/children")
    public List<ChildResponse> children(@PathVariable Long id) {
        return parentService.getChildren(id);
    }
}
