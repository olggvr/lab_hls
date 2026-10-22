package com.lab.grades.integration;

import org.springframework.stereotype.Component;

@Component
public class MessageClientFallback implements MessageClient {
    @Override
    public NotificationResponse sendGradeNotification(GradeNotificationRequest request) {
        return new NotificationResponse(null, 0, false);
    }
}
