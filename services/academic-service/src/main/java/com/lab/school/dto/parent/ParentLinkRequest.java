package com.lab.school.dto.parent;

import com.lab.school.entity.enums.ParentRelation;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ParentLinkRequest(
        @NotNull @Positive Long parentId,
        @NotNull ParentRelation relation
) {
}
