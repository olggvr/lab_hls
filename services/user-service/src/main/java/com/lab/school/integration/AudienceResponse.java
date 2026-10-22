package com.lab.school.integration;

import java.util.List;

public record AudienceResponse(Long senderId, String senderName, List<Long> recipientIds) {
}
