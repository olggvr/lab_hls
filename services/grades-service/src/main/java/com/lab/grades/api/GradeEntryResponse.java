package com.lab.grades.api;

import java.time.LocalDateTime;

public record GradeEntryResponse(
        Long submissionId, Long assignmentId, String assignmentTitle, String subjectName,
        Long studentId, String studentName, Integer grade, LocalDateTime gradedAt) {
}
