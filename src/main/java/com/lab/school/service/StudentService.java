package com.lab.school.service;

import com.lab.school.dto.parent.ParentContactResponse;
import com.lab.school.dto.parent.ParentLinkRequest;
import com.lab.school.dto.student.StudentCreateRequest;
import com.lab.school.dto.student.StudentResponse;
import com.lab.school.dto.student.StudentUpdateRequest;
import com.lab.school.entity.Parent;
import com.lab.school.entity.ParentStudent;
import com.lab.school.entity.Student;
import com.lab.school.entity.User;
import com.lab.school.entity.enums.UserRole;
import com.lab.school.exception.ConflictException;
import com.lab.school.exception.NotFoundException;
import com.lab.school.mapper.ParentMapper;
import com.lab.school.mapper.StudentMapper;
import com.lab.school.repository.ParentStudentRepository;
import com.lab.school.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final StudentMapper studentMapper;
    private final ParentMapper parentMapper;
    private final EntityFinder finder;

    public Page<StudentResponse> findAll(int page, int size) {
        return studentRepository.findAll(PageRequests.byId(page, size)).map(studentMapper::toResponse);
    }

    public StudentResponse getById(Long id) {
        return studentMapper.toResponse(finder.student(id));
    }

    @Transactional
    public StudentResponse create(StudentCreateRequest request) {
        User user = finder.user(request.userId());
        ProfileGuard.requireRole(user, UserRole.STUDENT);
        if (studentRepository.existsByUserId(user.getId())) {
            throw new ConflictException("Профиль ученика для пользователя id=" + user.getId() + " уже существует");
        }
        Student student = new Student();
        student.setUser(user);
        student.setSchoolClass(finder.schoolClass(request.classId()));
        return studentMapper.toResponse(studentRepository.save(student));
    }

    /**
     * Перевод ученика в другой класс.
     */
    @Transactional
    public StudentResponse update(Long id, StudentUpdateRequest request) {
        Student student = finder.student(id);
        student.setSchoolClass(finder.schoolClass(request.classId()));
        return studentMapper.toResponse(student);
    }

    @Transactional
    public void delete(Long id) {
        studentRepository.delete(finder.student(id));
        studentRepository.flush();
    }

    /**
     * Контакты родителей ученика.
     */
    public List<ParentContactResponse> getParents(Long studentId) {
        finder.student(studentId);
        return parentStudentRepository.findByStudentId(studentId).stream()
                .map(parentMapper::toContact)
                .toList();
    }

    @Transactional
    public ParentContactResponse linkParent(Long studentId, ParentLinkRequest request) {
        Student student = finder.student(studentId);
        Parent parent = finder.parent(request.parentId());
        if (parentStudentRepository.existsByParentIdAndStudentId(parent.getId(), studentId)) {
            throw new ConflictException("Родитель id=" + parent.getId() + " уже привязан к ученику id=" + studentId);
        }
        ParentStudent link = parentStudentRepository.save(new ParentStudent(parent, student, request.relation()));
        return parentMapper.toContact(link);
    }

    @Transactional
    public void unlinkParent(Long studentId, Long parentId) {
        ParentStudent link = parentStudentRepository.findByParentIdAndStudentId(parentId, studentId)
                .orElseThrow(() -> new NotFoundException(
                        "Связь ученика id=" + studentId + " с родителем id=" + parentId + " не найдена"));
        parentStudentRepository.delete(link);
    }
}
