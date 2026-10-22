package com.lab.school.mapper;

import com.lab.school.dto.assignment.AssignmentResponse;
import com.lab.school.dto.assignment.SubmissionResponse;
import com.lab.school.dto.user.GradeEntryResponse;
import com.lab.school.entity.Assignment;
import com.lab.school.entity.Submission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface AssignmentMapper {

    @Mapping(target = "teacherId", source = "teacher.id")
    @Mapping(target = "teacherName", source = "teacher.user.fullName")
    @Mapping(target = "classId", source = "schoolClass.id")
    @Mapping(target = "className", source = "schoolClass.name")
    @Mapping(target = "subjectId", source = "subject.id")
    @Mapping(target = "subjectName", source = "subject.name")
    AssignmentResponse toResponse(Assignment assignment);

    @Mapping(target = "assignmentId", source = "assignment.id")
    @Mapping(target = "studentId", source = "student.id")
    @Mapping(target = "studentName", source = "student.user.fullName")
    SubmissionResponse toSubmissionResponse(Submission submission);

    @Mapping(target = "submissionId", source = "id")
    @Mapping(target = "assignmentId", source = "assignment.id")
    @Mapping(target = "assignmentTitle", source = "assignment.title")
    @Mapping(target = "subjectName", source = "assignment.subject.name")
    @Mapping(target = "studentId", source = "student.id")
    @Mapping(target = "studentName", source = "student.user.fullName")
    GradeEntryResponse toGradeEntry(Submission submission);
}
