package com.lab.school.integration;

import com.lab.school.dto.message.BroadcastAudience;
import com.lab.school.dto.message.BroadcastScope;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BroadcastAudienceRequest(
        @NotNull @Positive Long senderId,
        @NotNull BroadcastScope scope,
        @Positive Long classId,
        @Min(1) @Max(11) Integer year,
        @NotNull BroadcastAudience audience
) {
}
