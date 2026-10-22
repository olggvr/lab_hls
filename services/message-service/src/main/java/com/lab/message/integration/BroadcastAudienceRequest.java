package com.lab.message.integration;

import com.lab.message.api.BroadcastAudience;
import com.lab.message.api.BroadcastScope;

public record BroadcastAudienceRequest(
        Long senderId, BroadcastScope scope, Long classId, Integer year, BroadcastAudience audience
) { }
