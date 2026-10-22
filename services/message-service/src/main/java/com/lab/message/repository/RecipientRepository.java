package com.lab.message.repository;

import com.lab.message.domain.RecipientEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RecipientRepository extends ReactiveCrudRepository<RecipientEntity, Long> {
    Flux<RecipientEntity> findByUserIdOrderByIdDesc(Long userId);
    Mono<RecipientEntity> findByMessageIdAndUserId(Long messageId, Long userId);
    Flux<RecipientEntity> findByMessageId(Long messageId);
    Mono<Void> deleteByMessageId(Long messageId);
}
