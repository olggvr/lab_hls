package com.lab.grades.integration;

import jakarta.validation.Valid;
import reactor.core.publisher.Mono;

public interface MessageClient {
    Mono<NotificationResponse> sendGradeNotification(@Valid GradeNotificationRequest request);
}
