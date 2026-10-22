package com.lab.school.integration;

import java.util.Set;

public record DirectAudienceRequest(Long senderId, Set<Long> recipientIds) {
}
