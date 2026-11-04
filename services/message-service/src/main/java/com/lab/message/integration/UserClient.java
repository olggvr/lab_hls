package com.lab.message.integration;

import com.lab.message.exception.DownstreamUnavailableException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class UserClient {
    private final WebClient webClient;
    private final ReactiveCircuitBreaker circuitBreaker;

    public UserClient(WebClient.Builder builder,
                      @Qualifier("userDirectoryCircuitBreaker") ReactiveCircuitBreaker circuitBreaker) {
        this.webClient = builder.clone().baseUrl("http://user-service/internal/directory").build();
        this.circuitBreaker = circuitBreaker;
    }

    public Mono<AudienceResponse> resolveDirect(DirectAudienceRequest request) {
        return execute(webClient.post().uri("/direct").bodyValue(request).retrieve()
                .bodyToMono(AudienceResponse.class));
    }

    public Mono<AudienceResponse> resolveBroadcast(BroadcastAudienceRequest request) {
        return execute(webClient.post().uri("/broadcast").bodyValue(request).retrieve()
                .bodyToMono(AudienceResponse.class));
    }

    private <T> Mono<T> execute(Mono<T> call) {
        return circuitBreaker.run(call, failure -> Mono.error(
                new DownstreamUnavailableException("User Service временно недоступен")));
    }
}
