package com.lab.message.integration;

import java.util.List;

public record AudienceResponse(Long senderId, String senderName, List<Long> recipientIds) { }
