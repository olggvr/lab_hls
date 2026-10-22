package com.lab.school.service;

import com.lab.school.dto.schoolclass.SchoolClassRequest;
import com.lab.school.dto.schoolclass.SchoolClassResponse;
import com.lab.school.entity.SchoolClass;
import com.lab.school.exception.ConflictException;
import com.lab.school.exception.NotFoundException;
import com.lab.school.mapper.SchoolClassMapper;
import com.lab.school.repository.SchoolClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Модуль классов. Единственный владелец classes.
 * Список учеников класса отдаёт модуль учеников ({@link StudentService#listStudentsOfClass(Long)}).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SchoolClassService {

    private final SchoolClassRepository schoolClassRepository;
    private final SchoolClassMapper schoolClassMapper;

    public Page<SchoolClassResponse> listClasses(int page, int size) {
        return schoolClassRepository.findAll(PageRequests.byId(page, size))
                .map(schoolClassMapper::toResponse);
    }

    public SchoolClassResponse getSchoolClass(Long id) {
        return schoolClassMapper.toResponse(requireSchoolClass(id));
    }

    @Transactional
    public SchoolClassResponse createSchoolClass(SchoolClassRequest request) {
        if (schoolClassRepository.existsByName(request.name())) {
            throw new ConflictException("Класс '" + request.name() + "' уже существует");
        }
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setName(request.name());
        schoolClass.setYear(request.year());
        return schoolClassMapper.toResponse(schoolClassRepository.save(schoolClass));
    }

    @Transactional
    public SchoolClassResponse updateSchoolClass(Long id, SchoolClassRequest request) {
        SchoolClass schoolClass = requireSchoolClass(id);
        if (schoolClassRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new ConflictException("Класс '" + request.name() + "' уже существует");
        }
        schoolClass.setName(request.name());
        schoolClass.setYear(request.year());
        return schoolClassMapper.toResponse(schoolClassRepository.saveAndFlush(schoolClass));
    }

    @Transactional
    public void deleteSchoolClass(Long id) {
        schoolClassRepository.delete(requireSchoolClass(id));
        schoolClassRepository.flush();
    }

    /**
     * Точка входа для других модулей: вернуть класс или 404.
     */
    public SchoolClass requireSchoolClass(Long id) {
        return schoolClassRepository.findById(id).orElseThrow(() -> new NotFoundException("Класс", id));
    }
}
