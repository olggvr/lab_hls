package com.lab.grades.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "grades", uniqueConstraints = @UniqueConstraint(name = "uq_grades_submission", columnNames = "submission_id"))
@Getter
@Setter
public class GradeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "submission_id", nullable = false)
    private Long submissionId;
    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;
    @Column(name = "student_id", nullable = false)
    private Long studentId;
    @Column(name = "student_name", nullable = false, length = 150)
    private String studentName;
    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;
    @Column(name = "assignment_title", nullable = false, length = 200)
    private String assignmentTitle;
    @Column(name = "subject_name", nullable = false, length = 100)
    private String subjectName;
    @Column(name = "content_url", nullable = false, length = 500)
    private String contentUrl;
    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;
    @Column(name = "grade", nullable = false)
    private Integer grade;
    @Column(name = "graded_at", nullable = false)
    private LocalDateTime gradedAt;
}
