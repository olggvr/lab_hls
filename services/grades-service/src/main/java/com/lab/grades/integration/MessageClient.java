package com.lab.grades.integration;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Validated
public class MessageClient {
    private final WebClient webClient;
    private final ReactiveCircuitBreaker circuitBreaker;

    public MessageClient(WebClient.Builder builder,
                         @Qualifier("messageCircuitBreaker") ReactiveCircuitBreaker circuitBreaker) {
        this.webClient = builder.clone().baseUrl("http://message-service/internal/messages").build();
        this.circuitBreaker = circuitBreaker;
    }

    public Mono<NotificationResponse> sendGradeNotification(@Valid GradeNotificationRequest request) {
        Mono<NotificationResponse> call = webClient.post().uri("/grade-notifications")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(NotificationResponse.class);
        return circuitBreaker.run(call, failure -> Mono.just(new NotificationResponse(null, 0, false)));
    }
}
