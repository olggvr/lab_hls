package com.lab.grades.integration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record GradeNotificationRequest(
        @NotNull @Positive Long senderId,
        @NotBlank @Size(max = 150) String senderName,
        @NotBlank @Size(max = 200) String subject,
        @NotBlank @Size(max = 10000) String body,
        @NotEmpty @Size(max = 100) List<@NotNull @Positive Long> recipientIds) {
}
