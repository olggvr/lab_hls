package com.lab.school.service;

import com.lab.school.dto.parent.ChildResponse;
import com.lab.school.dto.parent.ParentContactResponse;
import com.lab.school.dto.parent.ParentLinkRequest;
import com.lab.school.entity.Parent;
import com.lab.school.entity.ParentStudent;
import com.lab.school.entity.Student;
import com.lab.school.exception.ConflictException;
import com.lab.school.exception.NotFoundException;
import com.lab.school.mapper.ParentMapper;
import com.lab.school.repository.ParentStudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParentStudentService {
    private final ParentStudentRepository repository;
    private final ParentService parentService;
    private final StudentService studentService;
    private final ParentMapper parentMapper;

    public List<ParentContactResponse> listParentsOfStudent(Long studentId) {
        studentService.requireStudent(studentId);
        return repository.findByStudentId(studentId).stream().map(parentMapper::toContact).toList();
    }

    public List<ChildResponse> listChildrenOfParent(Long parentId) {
        parentService.requireParent(parentId);
        return repository.findByParentId(parentId).stream().map(parentMapper::toChild).toList();
    }

    @Transactional
    public ParentContactResponse linkParent(Long studentId, ParentLinkRequest request) {
        Student student = studentService.requireStudent(studentId);
        Parent parent = parentService.requireParent(request.parentId());
        if (repository.existsByParentIdAndStudentId(parent.getId(), student.getId())) {
            throw new ConflictException("Родитель уже привязан к ученику");
        }
        return parentMapper.toContact(repository.save(new ParentStudent(parent, student, request.relation())));
    }

    @Transactional
    public void unlinkParent(Long studentId, Long parentId) {
        ParentStudent link = repository.findByParentIdAndStudentId(parentId, studentId)
                .orElseThrow(() -> new NotFoundException("Связь родителя и ученика не найдена"));
        repository.delete(link);
    }

    public List<ParentStudent> collectLinksOfStudent(Long studentId) {
        return repository.findByStudentId(studentId);
    }

    public List<Long> collectChildIdsOfParent(Long parentId) {
        return repository.findByParentId(parentId).stream().map(link -> link.getStudent().getId()).toList();
    }

    public List<ParentStudent> collectLinksOfStudents(Collection<Long> studentIds) {
        return repository.findByStudentIdIn(studentIds);
    }
}
