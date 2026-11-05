package com.lab.grades.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
    @NotNull @Positive
    private Long submissionId;
    @Column(name = "assignment_id", nullable = false)
    @NotNull @Positive
    private Long assignmentId;
    @Column(name = "student_id", nullable = false)
    @NotNull @Positive
    private Long studentId;
    @Column(name = "student_name", nullable = false, length = 150)
    @NotBlank @Size(max = 150)
    private String studentName;
    @Column(name = "teacher_id", nullable = false)
    @NotNull @Positive
    private Long teacherId;
    @Column(name = "assignment_title", nullable = false, length = 200)
    @NotBlank @Size(max = 200)
    private String assignmentTitle;
    @Column(name = "subject_name", nullable = false, length = 100)
    @NotBlank @Size(max = 100)
    private String subjectName;
    @Column(name = "content_url", nullable = false, length = 500)
    @NotBlank @Size(max = 500)
    @Pattern(regexp = "^https?://.+", message = "должна быть ссылкой http(s)://")
    private String contentUrl;
    @Column(name = "submitted_at", nullable = false)
    @NotNull @PastOrPresent
    private LocalDateTime submittedAt;
    @Column(name = "grade", nullable = false)
    @NotNull @Min(1) @Max(5)
    private Integer grade;
    @Column(name = "graded_at", nullable = false)
    @NotNull @PastOrPresent
    private LocalDateTime gradedAt;
}
