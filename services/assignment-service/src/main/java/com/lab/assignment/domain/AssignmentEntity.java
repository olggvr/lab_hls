package com.lab.assignment.domain;

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
    private Long teacherId;
    private Long teacherUserId;
    private String teacherName;
    private Long classId;
    private String className;
    private Long subjectId;
    private String subjectName;
    private String title;
    private String description;
    private LocalDate dueDate;
    private LocalDateTime createdAt;
}
