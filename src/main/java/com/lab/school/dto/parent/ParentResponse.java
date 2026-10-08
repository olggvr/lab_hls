package com.lab.school.dto.parent;

public record ParentResponse(
        Long id,
        Long userId,
        String fullName,
        String email,
        String phone
) {
}
