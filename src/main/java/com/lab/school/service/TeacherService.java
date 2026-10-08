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
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final TeacherClassSubjectRepository workloadRepository;
    private final TeacherMapper teacherMapper;
    private final EntityFinder finder;

    public TeacherService(TeacherRepository teacherRepository,
                          TeacherClassSubjectRepository workloadRepository,
                          TeacherMapper teacherMapper,
                          EntityFinder finder) {
        this.teacherRepository = teacherRepository;
        this.workloadRepository = workloadRepository;
        this.teacherMapper = teacherMapper;
        this.finder = finder;
    }

    public Page<TeacherResponse> findAll(int page, int size) {
        return teacherRepository.findAll(PageRequests.byId(page, size)).map(teacherMapper::toResponse);
    }

    public TeacherResponse getById(Long id) {
        return teacherMapper.toResponse(finder.teacher(id));
    }

    @Transactional
    public TeacherResponse create(TeacherCreateRequest request) {
        User user = finder.user(request.userId());
        ProfileGuard.requireRole(user, UserRole.TEACHER);
        if (teacherRepository.existsByUserId(user.getId())) {
            throw new ConflictException("Профиль учителя для пользователя id=" + user.getId() + " уже существует");
        }
        Teacher teacher = new Teacher();
        teacher.setUser(user);
        teacher.setSubject(request.subjectId() == null ? null : finder.subject(request.subjectId()));
        return teacherMapper.toResponse(teacherRepository.save(teacher));
    }

    @Transactional
    public TeacherResponse update(Long id, TeacherUpdateRequest request) {
        Teacher teacher = finder.teacher(id);
        teacher.setSubject(request.subjectId() == null ? null : finder.subject(request.subjectId()));
        return teacherMapper.toResponse(teacher);
    }

    @Transactional
    public void delete(Long id) {
        teacherRepository.delete(finder.teacher(id));
        teacherRepository.flush();
    }

    public List<WorkloadResponse> getWorkload(Long teacherId) {
        finder.teacher(teacherId);
        return workloadRepository.findByTeacherId(teacherId).stream()
                .map(teacherMapper::toWorkloadResponse)
                .toList();
    }

    /**
     * Назначение учителя на класс по предмету на учебный год (M:N с доп. полем academic_year).
     */
    @Transactional
    public WorkloadResponse addWorkload(Long teacherId, WorkloadRequest request) {
        Teacher teacher = finder.teacher(teacherId);
        SchoolClass schoolClass = finder.schoolClass(request.classId());
        Subject subject = finder.subject(request.subjectId());
        if (workloadRepository.existsByTeacherIdAndSchoolClassIdAndSubjectIdAndAcademicYear(
                teacherId, schoolClass.getId(), subject.getId(), request.academicYear())) {
            throw new ConflictException("Учитель уже назначен на этот класс и предмет в "
                    + request.academicYear() + " учебном году");
        }
        TeacherClassSubject workload = new TeacherClassSubject(teacher, schoolClass, subject, request.academicYear());
        return teacherMapper.toWorkloadResponse(workloadRepository.save(workload));
    }

    @Transactional
    public void removeWorkload(Long teacherId, Long workloadId) {
        TeacherClassSubject workload = workloadRepository.findByIdAndTeacherId(workloadId, teacherId)
                .orElseThrow(() -> new NotFoundException("Назначение", workloadId));
        workloadRepository.delete(workload);
    }
}
