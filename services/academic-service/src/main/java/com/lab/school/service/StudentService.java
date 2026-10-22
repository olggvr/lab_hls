package com.lab.school.service;

import com.lab.school.dto.student.StudentCreateRequest;
import com.lab.school.dto.student.StudentResponse;
import com.lab.school.dto.student.StudentUpdateRequest;
import com.lab.school.entity.Student;
import com.lab.school.entity.User;
import com.lab.school.entity.enums.UserRole;
import com.lab.school.exception.ConflictException;
import com.lab.school.exception.NotFoundException;
import com.lab.school.mapper.StudentMapper;
import com.lab.school.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Модуль учеников. Единственный владелец students.
 * Пользователя берёт у {@link UserService}, класс — у {@link SchoolClassService}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserService userService;
    private final SchoolClassService schoolClassService;
    private final StudentMapper studentMapper;

    public Page<StudentResponse> listStudents(int page, int size) {
        return studentRepository.findAll(PageRequests.byId(page, size)).map(studentMapper::toResponse);
    }

    public StudentResponse getStudent(Long id) {
        return studentMapper.toResponse(requireStudent(id));
    }

    @Transactional
    public StudentResponse createStudent(StudentCreateRequest request) {
        User user = userService.requireUser(request.userId());
        ProfileGuard.requireRole(user, UserRole.STUDENT);
        if (studentRepository.existsByUserId(user.getId())) {
            throw new ConflictException("Профиль ученика для пользователя id=" + user.getId() + " уже существует");
        }
        Student student = new Student();
        student.setUser(user);
        student.setSchoolClass(schoolClassService.requireSchoolClass(request.classId()));
        return studentMapper.toResponse(studentRepository.save(student));
    }

    /**
     * Перевод ученика в другой класс.
     */
    @Transactional
    public StudentResponse updateStudent(Long id, StudentUpdateRequest request) {
        Student student = requireStudent(id);
        student.setSchoolClass(schoolClassService.requireSchoolClass(request.classId()));
        return studentMapper.toResponse(student);
    }

    @Transactional
    public void deleteStudent(Long id) {
        studentRepository.delete(requireStudent(id));
        studentRepository.flush();
    }

    /**
     * Ученики класса (One-to-Many classes -&gt; students).
     */
    public List<StudentResponse> listStudentsOfClass(Long classId) {
        return collectStudentsOfClass(classId).stream()
                .map(studentMapper::toResponse)
                .toList();
    }

    /**
     * Точка входа для других модулей: вернуть ученика или 404.
     */
    public Student requireStudent(Long id) {
        return studentRepository.findById(id).orElseThrow(() -> new NotFoundException("Ученик", id));
    }

    public Optional<Student> findStudentByUserId(Long userId) {
        return studentRepository.findByUserId(userId);
    }

    public List<Student> collectStudentsOfClass(Long classId) {
        schoolClassService.requireSchoolClass(classId);
        return studentRepository.findBySchoolClassId(classId);
    }

    public List<Student> collectStudentsOfParallel(Integer year) {
        return studentRepository.findBySchoolClass_Year(year);
    }

    public List<Student> collectAllStudents() {
        return studentRepository.findAllBy();
    }
}
