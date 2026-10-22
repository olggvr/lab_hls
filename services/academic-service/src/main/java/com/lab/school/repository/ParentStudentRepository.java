package com.lab.school.repository;

import com.lab.school.entity.ParentStudent;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ParentStudentRepository extends JpaRepository<ParentStudent, Long> {

    @EntityGraph(attributePaths = {"parent", "parent.user"})
    List<ParentStudent> findByStudentId(Long studentId);

    @EntityGraph(attributePaths = {"parent", "parent.user"})
    List<ParentStudent> findByStudentIdIn(Collection<Long> studentIds);

    @EntityGraph(attributePaths = {"student", "student.user", "student.schoolClass"})
    List<ParentStudent> findByParentId(Long parentId);

    Optional<ParentStudent> findByParentIdAndStudentId(Long parentId, Long studentId);

    boolean existsByParentIdAndStudentId(Long parentId, Long studentId);
}
