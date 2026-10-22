package com.lab.school.integration;

import java.util.List;

public record GradeAudienceResponse(Long studentId, String studentName, List<Long> recipientIds) {
}
