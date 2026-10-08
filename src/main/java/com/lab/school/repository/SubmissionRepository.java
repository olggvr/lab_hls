package com.lab.school.repository;

import com.lab.school.entity.Submission;
import com.lab.school.entity.enums.SubmissionStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    Optional<Submission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    boolean existsByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    @EntityGraph(attributePaths = {"student", "student.user"})
    List<Submission> findByAssignmentIdOrderByIdAsc(Long assignmentId);

    @EntityGraph(attributePaths = {"assignment", "assignment.subject", "student", "student.user"})
    List<Submission> findByStudentIdInAndStatusOrderByGradedAtDesc(Collection<Long> studentIds, SubmissionStatus status);

    @EntityGraph(attributePaths = {"assignment", "assignment.subject", "student", "student.user"})
    List<Submission> findByAssignment_Teacher_IdAndStatusOrderByGradedAtDesc(Long teacherId, SubmissionStatus status);
}
