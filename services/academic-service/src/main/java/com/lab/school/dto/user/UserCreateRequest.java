package com.lab.school.dto.user;

import com.lab.school.entity.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank @Size(min = 3, max = 50)
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "может содержать только латиницу, цифры и . _ -")
        String username,

        @NotBlank @Size(max = 150)
        String fullName,

        @NotBlank @Email @Size(max = 150)
        String email,

        @Pattern(regexp = "^\\+?[0-9 ()-]{7,20}$", message = "некорректный номер телефона")
        String phone,

        @NotNull
        UserRole role
) {
}
