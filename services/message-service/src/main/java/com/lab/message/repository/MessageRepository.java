package com.lab.message.repository;

import com.lab.message.domain.MessageEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface MessageRepository extends ReactiveCrudRepository<MessageEntity, Long> {
}
