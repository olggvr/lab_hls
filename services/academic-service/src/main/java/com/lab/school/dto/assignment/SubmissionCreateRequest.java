package com.lab.school.dto.assignment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SubmissionCreateRequest(
        @NotNull @Positive Long studentId,
        @NotBlank @Size(max = 500)
        @Pattern(regexp = "^https?://.+", message = "должна быть ссылкой http(s)://")
        String contentUrl
) {
}
