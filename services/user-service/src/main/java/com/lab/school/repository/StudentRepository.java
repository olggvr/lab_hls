package com.lab.school.repository;

import com.lab.school.entity.Student;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    @Override
    @EntityGraph(attributePaths = {"user", "schoolClass"})
    Optional<Student> findById(Long id);

    boolean existsByUserId(Long userId);

    Optional<Student> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"user", "schoolClass"})
    List<Student> findBySchoolClassId(Long classId);

    @EntityGraph(attributePaths = {"user", "schoolClass"})
    List<Student> findBySchoolClass_Year(Integer year);

    @EntityGraph(attributePaths = {"user", "schoolClass"})
    List<Student> findAllBy();
}
