package com.lab.school.service;

import com.lab.school.dto.schoolclass.SchoolClassRequest;
import com.lab.school.dto.schoolclass.SchoolClassResponse;
import com.lab.school.entity.SchoolClass;
import com.lab.school.exception.ConflictException;
import com.lab.school.mapper.SchoolClassMapper;
import com.lab.school.repository.SchoolClassRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SchoolClassService {

    private final SchoolClassRepository schoolClassRepository;
    private final SchoolClassMapper schoolClassMapper;
    private final EntityFinder finder;

    public SchoolClassService(SchoolClassRepository schoolClassRepository,
                              SchoolClassMapper schoolClassMapper,
                              EntityFinder finder) {
        this.schoolClassRepository = schoolClassRepository;
        this.schoolClassMapper = schoolClassMapper;
        this.finder = finder;
    }

    public Page<SchoolClassResponse> findAll(int page, int size) {
        return schoolClassRepository.findAll(PageRequests.byId(page, size)).map(schoolClassMapper::toResponse);
    }

    public SchoolClassResponse getById(Long id) {
        return schoolClassMapper.toResponse(finder.schoolClass(id));
    }

    @Transactional
    public SchoolClassResponse create(SchoolClassRequest request) {
        if (schoolClassRepository.existsByName(request.name())) {
            throw new ConflictException("Класс '" + request.name() + "' уже существует");
        }
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setName(request.name());
        schoolClass.setYear(request.year());
        return schoolClassMapper.toResponse(schoolClassRepository.save(schoolClass));
    }

    @Transactional
    public SchoolClassResponse update(Long id, SchoolClassRequest request) {
        SchoolClass schoolClass = finder.schoolClass(id);
        if (schoolClassRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new ConflictException("Класс '" + request.name() + "' уже существует");
        }
        schoolClass.setName(request.name());
        schoolClass.setYear(request.year());
        return schoolClassMapper.toResponse(schoolClassRepository.saveAndFlush(schoolClass));
    }

    @Transactional
    public void delete(Long id) {
        schoolClassRepository.delete(finder.schoolClass(id));
        schoolClassRepository.flush();
    }
}
