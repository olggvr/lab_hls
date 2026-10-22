package com.lab.school.integration;

public record StudentReferenceResponse(
        Long studentId,
        Long studentUserId,
        String studentName,
        Long classId) {
}
