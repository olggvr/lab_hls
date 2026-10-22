package com.lab.message.integration;

import java.util.Set;

public record DirectAudienceRequest(Long senderId, Set<Long> recipientIds) { }
