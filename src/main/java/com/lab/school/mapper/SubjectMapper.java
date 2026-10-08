package com.lab.school.mapper;

import com.lab.school.dto.subject.SubjectResponse;
import com.lab.school.entity.Subject;
import org.mapstruct.Mapper;

@Mapper
public interface SubjectMapper {

    SubjectResponse toResponse(Subject subject);
}
