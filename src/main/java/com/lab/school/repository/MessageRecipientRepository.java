package com.lab.school.repository;

import com.lab.school.entity.MessageRecipient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MessageRecipientRepository extends JpaRepository<MessageRecipient, Long> {

    @EntityGraph(attributePaths = {"message", "message.sender"})
    Page<MessageRecipient> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    Optional<MessageRecipient> findByMessageIdAndUserId(Long messageId, Long userId);
}
