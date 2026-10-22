package com.lab.message.service;

import com.lab.message.api.BroadcastRequest;
import com.lab.message.api.DirectMessageRequest;
import com.lab.message.api.GradeNotificationRequest;
import com.lab.message.api.InboxItemResponse;
import com.lab.message.api.MessageResponse;
import com.lab.message.api.NotificationResponse;
import com.lab.message.domain.MessageEntity;
import com.lab.message.domain.MessageType;
import com.lab.message.domain.RecipientEntity;
import com.lab.message.exception.NotFoundException;
import com.lab.message.integration.UserClient;
import com.lab.message.integration.AudienceResponse;
import com.lab.message.integration.BroadcastAudienceRequest;
import com.lab.message.integration.DirectAudienceRequest;
import com.lab.message.repository.MessageRepository;
import com.lab.message.repository.RecipientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReactiveMessageService {
    private final MessageRepository messageRepository;
    private final RecipientRepository recipientRepository;
    private final UserClient userClient;
    private final TransactionalOperator transactionalOperator;

    public Mono<MessageResponse> sendDirect(DirectMessageRequest request) {
        return Mono.fromCallable(() -> userClient.resolveDirect(
                        new DirectAudienceRequest(request.senderId(), request.recipientIds())))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(audience -> saveMessage(audience, request.subject(), request.body(), MessageType.DIRECT));
    }

    public Mono<MessageResponse> broadcast(BroadcastRequest request) {
        BroadcastAudienceRequest audienceRequest = new BroadcastAudienceRequest(
                request.senderId(), request.scope(), request.classId(), request.year(), request.audience());
        return Mono.fromCallable(() -> userClient.resolveBroadcast(audienceRequest))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(audience -> saveMessage(audience, request.subject(), request.body(), MessageType.BROADCAST));
    }

    public Mono<NotificationResponse> notifyGrade(GradeNotificationRequest request) {
        AudienceResponse audience = new AudienceResponse(
                request.senderId(), request.senderName(), request.recipientIds());
        return saveMessage(audience, request.subject(), request.body(), MessageType.GRADE_NOTIFICATION)
                .map(message -> new NotificationResponse(message.id(), message.recipientsCount(), true));
    }

    public Mono<MessageResponse> getMessage(Long id) {
        return requireMessage(id).flatMap(message -> recipientRepository.findByMessageId(id).count()
                .map(count -> toResponse(message, count.intValue())));
    }

    public Flux<InboxItemResponse> listInbox(Long userId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 50));
        return recipientRepository.findByUserIdOrderByIdDesc(userId)
                .skip((long) safePage * safeSize)
                .take(safeSize)
                .concatMap(recipient -> requireMessage(recipient.messageId())
                        .map(message -> toInbox(message, recipient)));
    }

    @Transactional
    public Mono<InboxItemResponse> markRead(Long messageId, Long userId) {
        return recipientRepository.findByMessageIdAndUserId(messageId, userId)
                .switchIfEmpty(Mono.error(new NotFoundException("Сообщение не найдено во входящих пользователя")))
                .map(RecipientEntity::markRead)
                .flatMap(recipientRepository::save)
                .flatMap(recipient -> requireMessage(messageId).map(message -> toInbox(message, recipient)));
    }

    @Transactional
    public Mono<Void> deleteMessage(Long id) {
        return requireMessage(id)
                .flatMap(message -> recipientRepository.deleteByMessageId(id)
                        .then(messageRepository.delete(message)));
    }

    protected Mono<MessageResponse> saveMessage(
            AudienceResponse audience, String subject, String body, MessageType type) {
        MessageEntity message = new MessageEntity(
                audience.senderId(), audience.senderName(), subject, body, type);
        List<Long> recipientIds = List.copyOf(new LinkedHashSet<>(audience.recipientIds()));
        return messageRepository.save(message)
                .flatMap(saved -> recipientRepository.saveAll(
                                recipientIds.stream().map(id -> new RecipientEntity(saved.id(), id)).toList())
                        .collectList()
                        .map(recipients -> toResponse(saved, recipients.size())))
                .as(transactionalOperator::transactional);
    }

    private Mono<MessageEntity> requireMessage(Long id) {
        return messageRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Сообщение id=" + id + " не найдено")));
    }

    private MessageResponse toResponse(MessageEntity message, int count) {
        return new MessageResponse(message.id(), message.senderId(), message.senderName(), message.subject(),
                message.body(), message.type(), message.createdAt(), count);
    }

    private InboxItemResponse toInbox(MessageEntity message, RecipientEntity recipient) {
        return new InboxItemResponse(message.id(), message.senderId(), message.senderName(), message.subject(),
                message.body(), message.type(), message.createdAt(), recipient.read(), recipient.readAt());
    }
}
