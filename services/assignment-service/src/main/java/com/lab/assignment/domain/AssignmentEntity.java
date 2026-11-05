package com.lab.assignment.domain;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Table("assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentEntity {
    @Id
    private Long id;
    @NotNull @Positive
    private Long teacherId;
    @NotNull @Positive
    private Long teacherUserId;
    @NotBlank @Size(max = 150)
    private String teacherName;
    @NotNull @Positive
    private Long classId;
    @NotBlank @Size(max = 20)
    private String className;
    @NotNull @Positive
    private Long subjectId;
    @NotBlank @Size(max = 100)
    private String subjectName;
    @NotBlank @Size(max = 200)
    private String title;
    @Size(max = 5000)
    private String description;
    @NotNull @FutureOrPresent
    private LocalDate dueDate;
    @NotNull @PastOrPresent
    private LocalDateTime createdAt;
}
