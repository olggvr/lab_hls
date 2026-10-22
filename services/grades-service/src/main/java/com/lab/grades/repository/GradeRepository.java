package com.lab.grades.repository;

import com.lab.grades.domain.GradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GradeRepository extends JpaRepository<GradeEntity, Long> {
    Optional<GradeEntity> findBySubmissionId(Long submissionId);
    List<GradeEntity> findByStudentIdInOrderByGradedAtDesc(Collection<Long> studentIds);
    List<GradeEntity> findByTeacherIdOrderByGradedAtDesc(Long teacherId);
}
