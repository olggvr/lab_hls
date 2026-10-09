package com.lab.school.controller;

import com.lab.school.dto.user.GradeEntryResponse;
import com.lab.school.dto.user.RoleRequest;
import com.lab.school.dto.user.UserCreateRequest;
import com.lab.school.dto.user.UserResponse;
import com.lab.school.dto.user.UserUpdateRequest;
import com.lab.school.entity.enums.UserRole;
import com.lab.school.service.GradeService;
import com.lab.school.service.UserService;
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

@Tag(name = "Users", description = "Пользователи и их роли")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final GradeService gradeService;

    @Operation(summary = "Список пользователей (постранично, не более 50)")
    @GetMapping
    public ResponseEntity<List<UserResponse>> listUsers(
            @RequestParam(defaultValue = DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return ResponseEntity.ok(userService.listUsers(page, size).getContent());
    }

    @Operation(summary = "Пользователь по id")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    @Operation(summary = "Создать пользователя")
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        UserResponse created = userService.createUser(request);
        return ResponseEntity.created(Locations.buildFor(created.id())).body(created);
    }

    @Operation(summary = "Обновить ФИО, email и телефон")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
                                                   @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @Operation(summary = "Удалить пользователя")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Добавить пользователю роль (M:N users <-> roles)")
    @PostMapping("/{id}/roles")
    public ResponseEntity<UserResponse> addRole(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        return ResponseEntity.ok(userService.addRole(id, request.role()));
    }

    @Operation(summary = "Снять с пользователя дополнительную роль")
    @DeleteMapping("/{id}/roles/{role}")
    public ResponseEntity<UserResponse> removeRole(@PathVariable Long id, @PathVariable UserRole role) {
        return ResponseEntity.ok(userService.removeRole(id, role));
    }

    @Operation(summary = "Оценки пользователя: ученик — свои, родитель — детей, учитель — по своим заданиям")
    @GetMapping("/{id}/grades")
    public ResponseEntity<List<GradeEntryResponse>> listGradesOfUser(@PathVariable Long id) {
        return ResponseEntity.ok(gradeService.listGradesOfUser(id));
    }
}
