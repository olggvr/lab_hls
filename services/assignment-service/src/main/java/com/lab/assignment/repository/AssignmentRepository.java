package com.lab.assignment.repository;

import com.lab.assignment.domain.AssignmentEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface AssignmentRepository extends ReactiveCrudRepository<AssignmentEntity, Long> {
    Flux<AssignmentEntity> findAllByOrderByIdDesc(Pageable pageable);
    Flux<AssignmentEntity> findByIdLessThanOrderByIdDesc(Long cursor, Pageable pageable);
}
