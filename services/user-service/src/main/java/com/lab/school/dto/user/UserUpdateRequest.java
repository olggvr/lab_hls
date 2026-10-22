package com.lab.school.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @NotBlank @Size(max = 150)
        String fullName,

        @NotBlank @Email @Size(max = 150)
        String email,

        @Pattern(regexp = "^\\+?[0-9 ()-]{7,20}$", message = "некорректный номер телефона")
        String phone
) {
}
