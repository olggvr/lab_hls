package com.lab.school.repository;

import com.lab.school.entity.Assignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    /**
     * Первая "страница" ленты (infinite scroll) — самые новые задания.
     */
    @EntityGraph(attributePaths = {"teacher", "teacher.user", "schoolClass", "subject"})
    Slice<Assignment> findAllByOrderByIdDesc(Pageable pageable);

    /**
     * Следующие "страницы" ленты: keyset-пагинация по id, без подсчёта total.
     */
    @EntityGraph(attributePaths = {"teacher", "teacher.user", "schoolClass", "subject"})
    Slice<Assignment> findByIdLessThanOrderByIdDesc(Long cursor, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"teacher", "teacher.user", "schoolClass", "subject"})
    Page<Assignment> findAll(Pageable pageable);
}
