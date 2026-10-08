package com.lab.school.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record DirectMessageRequest(
        @NotNull @Positive Long senderId,
        @NotEmpty @Size(max = 100) Set<@NotNull @Positive Long> recipientIds,
        @NotBlank @Size(max = 200) String subject,
        @NotBlank @Size(max = 10000) String body
) {
}
