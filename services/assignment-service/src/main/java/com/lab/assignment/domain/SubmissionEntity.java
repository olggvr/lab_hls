package com.lab.assignment.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
    @NotNull @Positive
    private Long assignmentId;
    @NotNull @Positive
    private Long studentId;
    @NotNull @Positive
    private Long studentUserId;
    @NotBlank @Size(max = 150)
    private String studentName;
    @NotBlank @Size(max = 500)
    @Pattern(regexp = "^https?://.+", message = "должна быть ссылкой http(s)://")
    private String contentUrl;
    @NotNull @PastOrPresent
    private LocalDateTime submittedAt;
    @NotBlank @Pattern(regexp = "SUBMITTED")
    private String status;
}
