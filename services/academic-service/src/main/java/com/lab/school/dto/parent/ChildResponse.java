package com.lab.school.dto.parent;

import com.lab.school.entity.enums.ParentRelation;

public record ChildResponse(
        Long studentId,
        String fullName,
        Long classId,
        String className,
        ParentRelation relation
) {
}
