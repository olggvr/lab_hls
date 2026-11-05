package com.lab.grades.service;

import com.lab.grades.domain.GradeEntity;
import com.lab.grades.integration.GradeScopeResponse;
import com.lab.grades.integration.GradingContextResponse;
import com.lab.grades.repository.GradeRepository;
import com.lab.grades.validation.DomainValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradePersistenceServiceTest {
    @Mock
    private GradeRepository repository;
    @Mock
    private DomainValidator domainValidator;

    @InjectMocks
    private GradePersistenceService service;

    @Test
    void createsGradeFromCompleteAssignmentContext() {
        GradingContextResponse context = context(100L);
        when(repository.findBySubmissionId(100L)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(GradeEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GradeEntity saved = service.save(context, 5);

        assertThat(saved.getSubmissionId()).isEqualTo(100L);
        assertThat(saved.getAssignmentId()).isEqualTo(1L);
        assertThat(saved.getStudentId()).isEqualTo(10L);
        assertThat(saved.getStudentName()).isEqualTo("Student");
        assertThat(saved.getTeacherId()).isEqualTo(20L);
        assertThat(saved.getAssignmentTitle()).isEqualTo("Homework");
        assertThat(saved.getSubjectName()).isEqualTo("Math");
        assertThat(saved.getContentUrl()).isEqualTo("https://files.test/solution.pdf");
        assertThat(saved.getGrade()).isEqualTo(5);
        assertThat(saved.getGradedAt()).isNotNull();
    }

    @Test
    void updatesExistingGradeForSameSubmission() {
        GradeEntity existing = new GradeEntity();
        existing.setId(77L);
        existing.setGrade(3);
        when(repository.findBySubmissionId(100L)).thenReturn(Optional.of(existing));
        when(repository.saveAndFlush(existing)).thenReturn(existing);

        GradeEntity saved = service.save(context(100L), 4);

        assertThat(saved).isSameAs(existing);
        assertThat(saved.getId()).isEqualTo(77L);
        assertThat(saved.getGrade()).isEqualTo(4);
    }

    @Test
    void findsGradesForStudentAndParentScopes() {
        GradeEntity studentGrade = grade(1L);
        GradeEntity parentGrade = grade(2L);
        when(repository.findByStudentIdInOrderByGradedAtDesc(List.of(10L)))
                .thenReturn(List.of(studentGrade));
        when(repository.findByStudentIdInOrderByGradedAtDesc(List.of(10L, 11L)))
                .thenReturn(List.of(studentGrade, parentGrade));

        assertThat(service.findForScope(new GradeScopeResponse("STUDENT", List.of(10L), null)))
                .containsExactly(studentGrade);
        assertThat(service.findForScope(new GradeScopeResponse("PARENT", List.of(10L, 11L), null)))
                .containsExactly(studentGrade, parentGrade);
    }

    @Test
    void emptyStudentScopeDoesNotQueryRepository() {
        assertThat(service.findForScope(new GradeScopeResponse("STUDENT", List.of(), null))).isEmpty();

        verifyNoInteractions(repository);
    }

    @Test
    void findsGradesForTeacherScope() {
        GradeEntity grade = grade(3L);
        when(repository.findByTeacherIdOrderByGradedAtDesc(20L)).thenReturn(List.of(grade));

        assertThat(service.findForScope(new GradeScopeResponse("TEACHER", List.of(), 20L)))
                .containsExactly(grade);
    }

    @Test
    void missingTeacherAndUnknownRoleReturnEmptyList() {
        assertThat(service.findForScope(new GradeScopeResponse("TEACHER", List.of(), null))).isEmpty();
        assertThat(service.findForScope(new GradeScopeResponse("SUPERVISOR", List.of(), null))).isEmpty();

        verify(repository, never()).findByTeacherIdOrderByGradedAtDesc(any());
    }

    private static GradingContextResponse context(Long submissionId) {
        return new GradingContextResponse(
                submissionId, 1L, 10L, "Student", "https://files.test/solution.pdf",
                LocalDateTime.of(2026, 11, 4, 12, 0), 20L, 2L, "Teacher", "Homework", "Math");
    }

    private static GradeEntity grade(Long id) {
        GradeEntity grade = new GradeEntity();
        grade.setId(id);
        return grade;
    }
}
