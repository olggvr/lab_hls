package com.lab.message.api;

import com.lab.message.service.ReactiveMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/messages")
public class InternalMessageController {
    private final ReactiveMessageService messageService;

    @PostMapping("/grade-notifications")
    public Mono<NotificationResponse> notifyGrade(@Valid @RequestBody GradeNotificationRequest request) {
        return messageService.notifyGrade(request);
    }
}
