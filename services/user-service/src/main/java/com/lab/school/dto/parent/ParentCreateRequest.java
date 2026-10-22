package com.lab.school.dto.parent;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ParentCreateRequest(@NotNull @Positive Long userId) {
}
