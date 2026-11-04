package com.lab.message.integration;

import com.lab.message.exception.DownstreamUnavailableException;
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

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class UserClientTest {
    @Test
    void resolvesAudienceLazilyThroughServiceDiscoveryUrl() {
        AtomicReference<ClientRequest> request = new AtomicReference<>();
        WebClient.Builder builder = WebClient.builder().exchangeFunction(actual -> {
            request.set(actual);
            return Mono.just(json("""
                    {"senderId":1,"senderName":"Teacher","recipientIds":[2,3]}
                    """));
        });
        UserClient client = new UserClient(builder, passThroughCircuitBreaker());

        Mono<AudienceResponse> result = client.resolveDirect(new DirectAudienceRequest(1L, Set.of(2L, 3L)));
        assertThat(request.get()).isNull();

        StepVerifier.create(result)
                .assertNext(response -> assertThat(response.recipientIds()).containsExactly(2L, 3L))
                .verifyComplete();
        assertThat(request.get().url().toString())
                .isEqualTo("http://user-service/internal/directory/direct");
    }

    @Test
    void mapsReactiveHttpFailureToServiceUnavailable() {
        UserClient client = new UserClient(
                WebClient.builder().exchangeFunction(request -> Mono.error(new IllegalStateException("offline"))),
                passThroughCircuitBreaker());

        StepVerifier.create(client.resolveDirect(new DirectAudienceRequest(1L, Set.of(2L))))
                .expectError(DownstreamUnavailableException.class)
                .verify();
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
