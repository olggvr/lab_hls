package com.lab.school.mapper;

import com.lab.school.dto.schoolclass.SchoolClassResponse;
import com.lab.school.entity.SchoolClass;
import org.mapstruct.Mapper;

@Mapper
public interface SchoolClassMapper {

    SchoolClassResponse toResponse(SchoolClass schoolClass);
}
