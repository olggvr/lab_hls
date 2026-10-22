package com.lab.school.dto.subject;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubjectRequest(@NotBlank @Size(max = 100) String name) {
}
