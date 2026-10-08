package com.lab.school.repository;

import com.lab.school.entity.TeacherClassSubject;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeacherClassSubjectRepository extends JpaRepository<TeacherClassSubject, Long> {

    @EntityGraph(attributePaths = {"schoolClass", "subject"})
    List<TeacherClassSubject> findByTeacherId(Long teacherId);

    Optional<TeacherClassSubject> findByIdAndTeacherId(Long id, Long teacherId);

    boolean existsByTeacherIdAndSchoolClassIdAndSubjectIdAndAcademicYear(
            Long teacherId, Long classId, Long subjectId, String academicYear);

    boolean existsByTeacherIdAndSchoolClassIdAndSubjectId(Long teacherId, Long classId, Long subjectId);
}
