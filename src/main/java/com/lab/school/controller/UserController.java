package com.lab.school.controller;

import com.lab.school.dto.user.RoleRequest;
import com.lab.school.dto.user.UserCreateRequest;
import com.lab.school.dto.user.UserResponse;
import com.lab.school.dto.user.UserUpdateRequest;
import com.lab.school.entity.enums.UserRole;
import com.lab.school.service.UserService;
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

@Tag(name = "Users", description = "Пользователи и их роли")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Список пользователей (постранично, не более 50)")
    @GetMapping
    public List<UserResponse> findAll(
            @RequestParam(defaultValue = DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return userService.findAll(page, size).getContent();
    }

    @Operation(summary = "Пользователь по id")
    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @Operation(summary = "Создать пользователя (пароль сохраняется в виде BCrypt-хеша)")
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        UserResponse created = userService.create(request);
        return ResponseEntity.created(Locations.of(created.id())).body(created);
    }

    @Operation(summary = "Обновить ФИО, email и телефон")
    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return userService.update(id, request);
    }

    @Operation(summary = "Удалить пользователя")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Добавить пользователю роль (M:N users <-> roles)")
    @PostMapping("/{id}/roles")
    public UserResponse addRole(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        return userService.addRole(id, request.role());
    }

    @Operation(summary = "Снять с пользователя дополнительную роль")
    @DeleteMapping("/{id}/roles/{role}")
    public UserResponse removeRole(@PathVariable Long id, @PathVariable UserRole role) {
        return userService.removeRole(id, role);
    }

}
