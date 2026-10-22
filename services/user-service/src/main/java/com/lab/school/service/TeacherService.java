package com.lab.school.service;

import com.lab.school.dto.teacher.TeacherCreateRequest;
import com.lab.school.dto.teacher.TeacherResponse;
import com.lab.school.dto.teacher.TeacherUpdateRequest;
import com.lab.school.dto.teacher.WorkloadRequest;
import com.lab.school.dto.teacher.WorkloadResponse;
import com.lab.school.entity.SchoolClass;
import com.lab.school.entity.Subject;
import com.lab.school.entity.Teacher;
import com.lab.school.entity.TeacherClassSubject;
import com.lab.school.entity.User;
import com.lab.school.entity.enums.UserRole;
import com.lab.school.exception.ConflictException;
import com.lab.school.exception.NotFoundException;
import com.lab.school.mapper.TeacherMapper;
import com.lab.school.repository.TeacherClassSubjectRepository;
import com.lab.school.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Модуль учителей. Единственный владелец teachers и teacher_class_subject (нагрузка).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final TeacherClassSubjectRepository workloadRepository;
    private final UserService userService;
    private final SubjectService subjectService;
    private final SchoolClassService schoolClassService;
    private final TeacherMapper teacherMapper;

    public Page<TeacherResponse> listTeachers(int page, int size) {
        return teacherRepository.findAll(PageRequests.byId(page, size)).map(teacherMapper::toResponse);
    }

    public TeacherResponse getTeacher(Long id) {
        return teacherMapper.toResponse(requireTeacher(id));
    }

    @Transactional
    public TeacherResponse createTeacher(TeacherCreateRequest request) {
        User user = userService.requireUser(request.userId());
        ProfileGuard.requireRole(user, UserRole.TEACHER);
        if (teacherRepository.existsByUserId(user.getId())) {
            throw new ConflictException("Профиль учителя для пользователя id=" + user.getId() + " уже существует");
        }
        Teacher teacher = new Teacher();
        teacher.setUser(user);
        teacher.setSubject(resolveSubject(request.subjectId()));
        return teacherMapper.toResponse(teacherRepository.save(teacher));
    }

    @Transactional
    public TeacherResponse updateTeacher(Long id, TeacherUpdateRequest request) {
        Teacher teacher = requireTeacher(id);
        teacher.setSubject(resolveSubject(request.subjectId()));
        return teacherMapper.toResponse(teacher);
    }

    @Transactional
    public void deleteTeacher(Long id) {
        teacherRepository.delete(requireTeacher(id));
        teacherRepository.flush();
    }

    public List<WorkloadResponse> listWorkload(Long teacherId) {
        requireTeacher(teacherId);
        return workloadRepository.findByTeacherId(teacherId).stream()
                .map(teacherMapper::toWorkloadResponse)
                .toList();
    }

    /**
     * Назначение учителя на класс по предмету на учебный год (M:N с доп. полем academic_year).
     */
    @Transactional
    public WorkloadResponse assignWorkload(Long teacherId, WorkloadRequest request) {
        Teacher teacher = requireTeacher(teacherId);
        SchoolClass schoolClass = schoolClassService.requireSchoolClass(request.classId());
        Subject subject = subjectService.requireSubject(request.subjectId());
        if (workloadRepository.existsByTeacherIdAndSchoolClassIdAndSubjectIdAndAcademicYear(
                teacherId, schoolClass.getId(), subject.getId(), request.academicYear())) {
            throw new ConflictException("Учитель уже назначен на этот класс и предмет в "
                    + request.academicYear() + " учебном году");
        }
        TeacherClassSubject workload = new TeacherClassSubject(teacher, schoolClass, subject, request.academicYear());
        return teacherMapper.toWorkloadResponse(workloadRepository.save(workload));
    }

    @Transactional
    public void revokeWorkload(Long teacherId, Long workloadId) {
        TeacherClassSubject workload = workloadRepository.findByIdAndTeacherId(workloadId, teacherId)
                .orElseThrow(() -> new NotFoundException("Назначение", workloadId));
        workloadRepository.delete(workload);
    }

    /**
     * Точка входа для других модулей: вернуть учителя или 404.
     */
    public Teacher requireTeacher(Long id) {
        return teacherRepository.findById(id).orElseThrow(() -> new NotFoundException("Учитель", id));
    }

    public Optional<Teacher> findTeacherByUserId(Long userId) {
        return teacherRepository.findByUserId(userId);
    }

    /**
     * Ведёт ли учитель этот предмет в этом классе — нужно модулю домашних заданий.
     */
    public boolean existsWorkload(Long teacherId, Long classId, Long subjectId) {
        return workloadRepository.existsByTeacherIdAndSchoolClassIdAndSubjectId(teacherId, classId, subjectId);
    }

    private Subject resolveSubject(Long subjectId) {
        return subjectId == null ? null : subjectService.requireSubject(subjectId);
    }
}
