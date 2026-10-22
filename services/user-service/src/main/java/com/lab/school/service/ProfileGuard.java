package com.lab.school.service;

import com.lab.school.entity.User;
import com.lab.school.entity.enums.UserRole;
import com.lab.school.exception.BusinessException;

final class ProfileGuard {

    private ProfileGuard() {
    }

    /**
     * Профиль учителя/ученика/родителя можно создать только для пользователя с соответствующей ролью.
     */
    static void requireRole(User user, UserRole expected) {
        if (user.getRole() != expected && !user.hasRole(expected)) {
            throw new BusinessException(
                    "Пользователь id=" + user.getId() + " не имеет роли " + expected);
        }
    }
}
