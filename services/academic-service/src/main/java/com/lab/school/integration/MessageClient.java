package com.lab.school.integration;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "message-service", path = "/internal/messages", fallback = MessageClientFallback.class)
public interface MessageClient {
    @PostMapping("/grade-notifications")
    NotificationResponse sendGradeNotification(@RequestBody GradeNotificationRequest request);
}
