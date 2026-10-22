package com.lab.assignment.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionEntity {
    @Id
    private Long id;
    private Long assignmentId;
    private Long studentId;
    private Long studentUserId;
    private String studentName;
    private String contentUrl;
    private LocalDateTime submittedAt;
    private String status;
}
