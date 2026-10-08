package com.lab.school.service;

import com.lab.school.dto.subject.SubjectRequest;
import com.lab.school.dto.subject.SubjectResponse;
import com.lab.school.entity.Subject;
import com.lab.school.exception.ConflictException;
import com.lab.school.mapper.SubjectMapper;
import com.lab.school.repository.SubjectRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final SubjectMapper subjectMapper;
    private final EntityFinder finder;

    public SubjectService(SubjectRepository subjectRepository, SubjectMapper subjectMapper, EntityFinder finder) {
        this.subjectRepository = subjectRepository;
        this.subjectMapper = subjectMapper;
        this.finder = finder;
    }

    public Page<SubjectResponse> findAll(int page, int size) {
        return subjectRepository.findAll(PageRequests.byId(page, size)).map(subjectMapper::toResponse);
    }

    public SubjectResponse getById(Long id) {
        return subjectMapper.toResponse(finder.subject(id));
    }

    @Transactional
    public SubjectResponse create(SubjectRequest request) {
        if (subjectRepository.existsByName(request.name())) {
            throw new ConflictException("Предмет '" + request.name() + "' уже существует");
        }
        Subject subject = new Subject();
        subject.setName(request.name());
        return subjectMapper.toResponse(subjectRepository.save(subject));
    }

    @Transactional
    public SubjectResponse update(Long id, SubjectRequest request) {
        Subject subject = finder.subject(id);
        if (subjectRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new ConflictException("Предмет '" + request.name() + "' уже существует");
        }
        subject.setName(request.name());
        return subjectMapper.toResponse(subjectRepository.saveAndFlush(subject));
    }

    @Transactional
    public void delete(Long id) {
        subjectRepository.delete(finder.subject(id));
        subjectRepository.flush();
    }
}
