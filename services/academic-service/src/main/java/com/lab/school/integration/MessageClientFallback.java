package com.lab.school.integration;

import org.springframework.stereotype.Component;

@Component
public class MessageClientFallback implements MessageClient {
    @Override
    public NotificationResponse sendGradeNotification(GradeNotificationRequest request) {
        return new NotificationResponse(null, 0, false);
    }
}
