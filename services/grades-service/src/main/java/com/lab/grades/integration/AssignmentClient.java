package com.lab.grades.integration;

import com.lab.grades.exception.DownstreamUnavailableException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class AssignmentClient {
    private final WebClient webClient;
    private final ReactiveCircuitBreaker circuitBreaker;

    public AssignmentClient(WebClient.Builder builder,
                            @Qualifier("assignmentCircuitBreaker") ReactiveCircuitBreaker circuitBreaker) {
        this.webClient = builder.clone().baseUrl("http://assignment-service/internal/assignments").build();
        this.circuitBreaker = circuitBreaker;
    }

    public Mono<GradingContextResponse> gradingContext(Long assignmentId, Long studentId) {
        Mono<GradingContextResponse> call = webClient.get()
                .uri("/{assignmentId}/submissions/by-student/{studentId}", assignmentId, studentId)
                .retrieve()
                .bodyToMono(GradingContextResponse.class);
        return circuitBreaker.run(call, failure -> Mono.error(
                new DownstreamUnavailableException("Assignment Service временно недоступен")));
    }
}
