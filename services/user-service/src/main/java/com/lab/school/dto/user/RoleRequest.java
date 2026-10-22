package com.lab.school.dto.user;

import com.lab.school.entity.enums.UserRole;
import jakarta.validation.constraints.NotNull;

public record RoleRequest(@NotNull UserRole role) {
}
