package com.lab.assignment.repository;

import com.lab.assignment.domain.SubmissionEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SubmissionRepository extends ReactiveCrudRepository<SubmissionEntity, Long> {
    Mono<SubmissionEntity> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);
    Mono<Boolean> existsByAssignmentIdAndStudentId(Long assignmentId, Long studentId);
    Flux<SubmissionEntity> findByAssignmentIdOrderByIdAsc(Long assignmentId);
    Mono<Void> deleteByAssignmentId(Long assignmentId);
}
