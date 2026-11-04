package com.lab.message;

import com.lab.message.api.BroadcastAudience;
import com.lab.message.api.BroadcastRequest;
import com.lab.message.api.BroadcastScope;
import com.lab.message.api.DirectMessageRequest;
import com.lab.message.api.GradeNotificationRequest;
import com.lab.message.integration.UserClient;
import com.lab.message.integration.AudienceResponse;
import com.lab.message.integration.DirectAudienceRequest;
import com.lab.message.exception.DownstreamUnavailableException;
import com.lab.message.service.ReactiveMessageService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "resilience4j.circuitbreaker.configs.default.slidingWindowSize=5",
        "resilience4j.circuitbreaker.configs.default.minimumNumberOfCalls=5",
        "resilience4j.circuitbreaker.configs.default.failureRateThreshold=50",
        "resilience4j.circuitbreaker.configs.default.waitDurationInOpenState=50ms",
        "resilience4j.circuitbreaker.configs.default.permittedNumberOfCallsInHalfOpenState=2"
})
@ActiveProfiles("test")
class ReactiveMessageServiceIntegrationTest {
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.4-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://" + POSTGRES.getHost() + ":"
                + POSTGRES.getMappedPort(5432) + "/" + POSTGRES.getDatabaseName());
        registry.add("spring.r2dbc.username", POSTGRES::getUsername);
        registry.add("spring.r2dbc.password", POSTGRES::getPassword);
    }

    @Autowired
    ReactiveMessageService service;

    @MockitoBean
    UserClient userClient;

    @Autowired
    @Qualifier("userDirectoryCircuitBreaker")
    ReactiveCircuitBreaker userDirectoryCircuitBreaker;

    @Autowired
    CircuitBreakerRegistry circuitBreakerRegistry;

    @Test
    void directMessageIsStoredAndAppearsInReactiveInbox() {
        when(userClient.resolveDirect(any()))
                .thenReturn(Mono.just(new AudienceResponse(1L, "Teacher", List.of(2L, 3L))));

        StepVerifier.create(service.sendDirect(new DirectMessageRequest(
                        1L, Set.of(2L, 3L), "Subject", "Body"))
                .flatMapMany(created -> service.listInbox(2L, 0, 20)))
                .assertNext(item -> {
                    assertThat(item.senderId()).isEqualTo(1L);
                    assertThat(item.subject()).isEqualTo("Subject");
                    assertThat(item.read()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    void supportsBroadcastGradeNotificationReadingAndDeleting() {
        when(userClient.resolveBroadcast(any()))
                .thenReturn(Mono.just(new AudienceResponse(1L, "Teacher", List.of(2L, 2L, 3L))));

        var broadcast = service.broadcast(new BroadcastRequest(
                1L, BroadcastScope.SCHOOL, null, null, BroadcastAudience.ALL,
                "Announcement", "School news")).block();

        assertThat(broadcast).isNotNull();
        assertThat(broadcast.recipientsCount()).isEqualTo(2);
        assertThat(service.getMessage(broadcast.id()).block().subject()).isEqualTo("Announcement");
        assertThat(service.markRead(broadcast.id(), 2L).block().read()).isTrue();

        var notification = service.notifyGrade(new GradeNotificationRequest(
                1L, "Teacher", "New grade", "Grade: 5", List.of(2L, 3L))).block();
        assertThat(notification.delivered()).isTrue();
        assertThat(notification.recipientsCount()).isEqualTo(2);

        service.deleteMessage(broadcast.id()).block();
        StepVerifier.create(service.getMessage(broadcast.id()))
                .expectErrorMessage("Сообщение id=" + broadcast.id() + " не найдено")
                .verify();
    }

    @Test
    void circuitBreakerOpensRejectsCallsAndRecoversThroughHalfOpen() throws InterruptedException {
        CircuitBreaker nativeBreaker = circuitBreakerRegistry.circuitBreaker("userDirectory");
        nativeBreaker.reset();
        AtomicBoolean backendAvailable = new AtomicBoolean(false);
        AtomicInteger backendCalls = new AtomicInteger();
        WebClient.Builder builder = WebClient.builder().exchangeFunction(request -> {
            backendCalls.incrementAndGet();
            if (!backendAvailable.get()) {
                return Mono.error(new IllegalStateException("User Service is offline"));
            }
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("{\"senderId\":1,\"senderName\":\"Teacher\",\"recipientIds\":[2]}")
                    .build());
        });
        UserClient realClient = new UserClient(builder, userDirectoryCircuitBreaker);
        DirectAudienceRequest request = new DirectAudienceRequest(1L, Set.of(2L));

        for (int attempt = 0; attempt < 6; attempt++) {
            assertThatThrownBy(() -> realClient.resolveDirect(request).block())
                    .isInstanceOf(DownstreamUnavailableException.class);
        }

        assertThat(nativeBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(backendCalls).hasValue(5);

        backendAvailable.set(true);
        Thread.sleep(100);
        StepVerifier.create(realClient.resolveDirect(request))
                .assertNext(audience -> assertThat(audience.recipientIds()).containsExactly(2L))
                .verifyComplete();
        assertThat(nativeBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
        StepVerifier.create(realClient.resolveDirect(request))
                .expectNextCount(1)
                .verifyComplete();
        assertThat(nativeBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }
}
