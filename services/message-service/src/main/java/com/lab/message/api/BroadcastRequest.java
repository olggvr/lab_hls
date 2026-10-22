package com.lab.message.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record BroadcastRequest(
        @NotNull @Positive Long senderId,
        @NotNull BroadcastScope scope,
        @Positive Long classId,
        @Min(1) @Max(11) Integer year,
        @NotNull BroadcastAudience audience,
        @NotBlank @Size(max = 200) String subject,
        @NotBlank @Size(max = 10000) String body
) { }
