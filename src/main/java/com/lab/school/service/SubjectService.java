package com.lab.school.service;

import com.lab.school.dto.subject.SubjectRequest;
import com.lab.school.dto.subject.SubjectResponse;
import com.lab.school.entity.Subject;
import com.lab.school.exception.ConflictException;
import com.lab.school.exception.NotFoundException;
import com.lab.school.mapper.SubjectMapper;
import com.lab.school.repository.SubjectRepository;
import com.lab.school.utils.PageRequests;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Модуль предметов. Единственный владелец subjects.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final SubjectMapper subjectMapper;

    public Page<SubjectResponse> listSubjects(int page, int size) {
        return subjectRepository.findAll(PageRequests.buildSortedById(page, size)).map(subjectMapper::toResponse);
    }

    public SubjectResponse getSubject(Long id) {
        return subjectMapper.toResponse(requireSubject(id));
    }

    @Transactional
    public SubjectResponse createSubject(SubjectRequest request) {
        if (subjectRepository.existsByName(request.name())) {
            throw new ConflictException("Предмет '" + request.name() + "' уже существует");
        }
        Subject subject = new Subject();
        subject.setName(request.name());
        return subjectMapper.toResponse(subjectRepository.save(subject));
    }

    @Transactional
    public SubjectResponse updateSubject(Long id, SubjectRequest request) {
        Subject subject = requireSubject(id);
        if (subjectRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new ConflictException("Предмет '" + request.name() + "' уже существует");
        }
        subject.setName(request.name());
        return subjectMapper.toResponse(subjectRepository.saveAndFlush(subject));
    }

    @Transactional
    public void deleteSubject(Long id) {
        subjectRepository.delete(requireSubject(id));
        subjectRepository.flush();
    }

    /**
     * Точка входа для других модулей: вернуть предмет или 404.
     */
    public Subject requireSubject(Long id) {
        return subjectRepository.findById(id).orElseThrow(() -> new NotFoundException("Предмет", id));
    }
}
