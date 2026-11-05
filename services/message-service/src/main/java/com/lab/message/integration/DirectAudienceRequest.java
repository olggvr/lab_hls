package com.lab.message.integration;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record DirectAudienceRequest(
        @NotNull @Positive Long senderId,
        @NotEmpty @Size(max = 100) Set<@NotNull @Positive Long> recipientIds) { }
