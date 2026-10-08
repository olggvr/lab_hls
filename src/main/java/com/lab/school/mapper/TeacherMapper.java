package com.lab.school.mapper;

import com.lab.school.dto.teacher.TeacherResponse;
import com.lab.school.dto.teacher.WorkloadResponse;
import com.lab.school.entity.Teacher;
import com.lab.school.entity.TeacherClassSubject;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface TeacherMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "fullName", source = "user.fullName")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "phone", source = "user.phone")
    @Mapping(target = "subjectId", source = "subject.id")
    @Mapping(target = "subjectName", source = "subject.name")
    TeacherResponse toResponse(Teacher teacher);

    @Mapping(target = "teacherId", source = "teacher.id")
    @Mapping(target = "classId", source = "schoolClass.id")
    @Mapping(target = "className", source = "schoolClass.name")
    @Mapping(target = "subjectId", source = "subject.id")
    @Mapping(target = "subjectName", source = "subject.name")
    WorkloadResponse toWorkloadResponse(TeacherClassSubject workload);
}
