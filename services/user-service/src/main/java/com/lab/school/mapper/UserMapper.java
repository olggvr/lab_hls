package com.lab.school.mapper;

import com.lab.school.dto.user.UserResponse;
import com.lab.school.entity.Role;
import com.lab.school.entity.User;
import com.lab.school.entity.enums.UserRole;
import org.mapstruct.Mapper;

@Mapper
public interface UserMapper {

    UserResponse toResponse(User user);

    default UserRole toRoleName(Role role) {
        return role == null ? null : role.getName();
    }
}
