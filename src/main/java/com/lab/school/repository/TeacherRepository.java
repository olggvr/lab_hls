package com.lab.school.repository;

import com.lab.school.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    boolean existsByUserId(Long userId);

    Optional<Teacher> findByUserId(Long userId);
}
