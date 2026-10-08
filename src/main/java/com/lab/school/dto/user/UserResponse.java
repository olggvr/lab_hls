package com.lab.school.dto.user;

import com.lab.school.entity.enums.UserRole;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Пароль (и его хеш) в ответах не отдаётся никогда.
 */
public record UserResponse(
        Long id,
        String username,
        String fullName,
        String email,
        String phone,
        UserRole role,
        Set<UserRole> roles,
        LocalDateTime createdAt
) {
}
