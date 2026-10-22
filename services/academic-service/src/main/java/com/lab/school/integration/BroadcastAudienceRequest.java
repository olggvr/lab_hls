package com.lab.school.integration;

import com.lab.school.dto.message.BroadcastAudience;
import com.lab.school.dto.message.BroadcastScope;

public record BroadcastAudienceRequest(
        Long senderId,
        BroadcastScope scope,
        Long classId,
        Integer year,
        BroadcastAudience audience
) {
}
