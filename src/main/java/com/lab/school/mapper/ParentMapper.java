package com.lab.school.mapper;

import com.lab.school.dto.parent.ChildResponse;
import com.lab.school.dto.parent.ParentContactResponse;
import com.lab.school.dto.parent.ParentResponse;
import com.lab.school.entity.Parent;
import com.lab.school.entity.ParentStudent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface ParentMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "fullName", source = "user.fullName")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "phone", source = "user.phone")
    ParentResponse toResponse(Parent parent);

    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "fullName", source = "parent.user.fullName")
    @Mapping(target = "email", source = "parent.user.email")
    @Mapping(target = "phone", source = "parent.user.phone")
    ParentContactResponse toContact(ParentStudent link);

    @Mapping(target = "studentId", source = "student.id")
    @Mapping(target = "fullName", source = "student.user.fullName")
    @Mapping(target = "classId", source = "student.schoolClass.id")
    @Mapping(target = "className", source = "student.schoolClass.name")
    ChildResponse toChild(ParentStudent link);
}
