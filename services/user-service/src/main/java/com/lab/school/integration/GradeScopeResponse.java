package com.lab.school.integration;

import java.util.List;

public record GradeScopeResponse(String role, List<Long> studentIds, Long teacherId) {
}
