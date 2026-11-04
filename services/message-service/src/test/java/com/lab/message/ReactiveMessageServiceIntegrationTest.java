package com.lab.message;

import com.lab.message.api.BroadcastAudience;
import com.lab.message.api.BroadcastRequest;
import com.lab.message.api.BroadcastScope;
import com.lab.message.api.DirectMessageRequest;
import com.lab.message.api.GradeNotificationRequest;
import com.lab.message.integration.UserClient;
import com.lab.message.integration.AudienceResponse;
import com.lab.message.service.ReactiveMessageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import reactor.test.StepVerifier;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
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
}
