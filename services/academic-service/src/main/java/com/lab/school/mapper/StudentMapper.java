package com.lab.school.mapper;

import com.lab.school.dto.student.StudentResponse;
import com.lab.school.entity.Student;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface StudentMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "fullName", source = "user.fullName")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "classId", source = "schoolClass.id")
    @Mapping(target = "className", source = "schoolClass.name")
    StudentResponse toResponse(Student student);
}
