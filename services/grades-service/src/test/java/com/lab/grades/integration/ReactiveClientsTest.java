package com.lab.grades.integration;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class ReactiveClientsTest {
    @Test
    void assignmentClientUsesReactiveServiceDiscoveryCall() {
        AtomicReference<ClientRequest> request = new AtomicReference<>();
        AssignmentClient client = new AssignmentClient(WebClient.builder().exchangeFunction(actual -> {
            request.set(actual);
            return Mono.just(json("""
                    {"submissionId":10,"assignmentId":1,"studentId":2,"studentName":"Student",
                     "contentUrl":"https://files.test/solution.pdf","teacherId":3,"teacherUserId":4,
                     "teacherName":"Teacher","assignmentTitle":"Homework","subjectName":"Math"}
                    """));
        }), passThroughCircuitBreaker());

        Mono<GradingContextResponse> result = client.gradingContext(1L, 2L);
        assertThat(request.get()).isNull();

        StepVerifier.create(result)
                .assertNext(response -> assertThat(response.submissionId()).isEqualTo(10L))
                .verifyComplete();
        assertThat(request.get().url().toString()).isEqualTo(
                "http://assignment-service/internal/assignments/1/submissions/by-student/2");
    }

    @Test
    void messageClientReturnsNonBlockingFallback() {
        MessageClient client = new MessageClient(
                WebClient.builder().exchangeFunction(request -> Mono.error(new IllegalStateException("offline"))),
                passThroughCircuitBreaker());

        StepVerifier.create(client.sendGradeNotification(new GradeNotificationRequest(
                        1L, "Teacher", "Grade", "Body", List.of(2L))))
                .assertNext(response -> {
                    assertThat(response.delivered()).isFalse();
                    assertThat(response.recipientsCount()).isZero();
                })
                .verifyComplete();
    }

    private static ClientResponse json(String body) {
        return ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build();
    }

    private static ReactiveCircuitBreaker passThroughCircuitBreaker() {
        return new ReactiveCircuitBreaker() {
            @Override
            public <T> Mono<T> run(Mono<T> publisher, Function<Throwable, Mono<T>> fallback) {
                return publisher.onErrorResume(fallback);
            }

            @Override
            public <T> Flux<T> run(Flux<T> publisher, Function<Throwable, Flux<T>> fallback) {
                return publisher.onErrorResume(fallback);
            }
        };
    }
}
