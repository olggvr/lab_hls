package com.lab.grades.service;

import com.lab.grades.domain.GradeEntity;
import com.lab.grades.integration.GradeScopeResponse;
import com.lab.grades.integration.GradingContextResponse;
import com.lab.grades.repository.GradeRepository;
import com.lab.grades.validation.DomainValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GradePersistenceService {
    private final GradeRepository gradeRepository;
    private final DomainValidator domainValidator;

    @Transactional
    public GradeEntity save(GradingContextResponse context, int value) {
        GradeEntity grade = gradeRepository.findBySubmissionId(context.submissionId()).orElseGet(GradeEntity::new);
        grade.setSubmissionId(context.submissionId());
        grade.setAssignmentId(context.assignmentId());
        grade.setStudentId(context.studentId());
        grade.setStudentName(context.studentName());
        grade.setTeacherId(context.teacherId());
        grade.setAssignmentTitle(context.assignmentTitle());
        grade.setSubjectName(context.subjectName());
        grade.setContentUrl(context.contentUrl());
        grade.setSubmittedAt(context.submittedAt());
        grade.setGrade(value);
        grade.setGradedAt(LocalDateTime.now());
        domainValidator.validate(grade);
        return gradeRepository.saveAndFlush(grade);
    }

    @Transactional(readOnly = true)
    public List<GradeEntity> findForScope(GradeScopeResponse scope) {
        return switch (scope.role()) {
            case "STUDENT", "PARENT" -> scope.studentIds().isEmpty()
                    ? List.of()
                    : gradeRepository.findByStudentIdInOrderByGradedAtDesc(scope.studentIds());
            case "TEACHER" -> scope.teacherId() == null
                    ? List.of()
                    : gradeRepository.findByTeacherIdOrderByGradedAtDesc(scope.teacherId());
            default -> List.of();
        };
    }
}
