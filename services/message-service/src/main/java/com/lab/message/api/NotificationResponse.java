package com.lab.message.api;

public record NotificationResponse(Long messageId, int recipientsCount, boolean delivered) { }
