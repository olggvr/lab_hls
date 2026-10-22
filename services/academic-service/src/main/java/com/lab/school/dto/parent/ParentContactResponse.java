package com.lab.school.dto.parent;

import com.lab.school.entity.enums.ParentRelation;

/**
 * Контакты родителя ученика (для учителя).
 */
public record ParentContactResponse(
        Long parentId,
        String fullName,
        String email,
        String phone,
        ParentRelation relation
) {
}
