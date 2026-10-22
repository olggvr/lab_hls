package com.lab.school.service;

import com.lab.school.dto.assignment.AssignmentCreateRequest;
import com.lab.school.dto.assignment.AssignmentResponse;
import com.lab.school.dto.assignment.AssignmentUpdateRequest;
import com.lab.school.dto.assignment.SubmissionCreateRequest;
import com.lab.school.dto.assignment.SubmissionResponse;
import com.lab.school.dto.common.CursorPage;
import com.lab.school.entity.Assignment;
import com.lab.school.entity.SchoolClass;
import com.lab.school.entity.Student;
import com.lab.school.entity.Subject;
import com.lab.school.entity.Submission;
import com.lab.school.entity.Teacher;
import com.lab.school.entity.enums.SubmissionStatus;
import com.lab.school.exception.BusinessException;
import com.lab.school.exception.ConflictException;
import com.lab.school.exception.NotFoundException;
import com.lab.school.mapper.AssignmentMapper;
import com.lab.school.repository.AssignmentRepository;
import com.lab.school.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Модуль домашних заданий. Единственный владелец assignments и submissions.
 * Учителя, классы, предметы и учеников берёт у соответствующих сервисов.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final TeacherService teacherService;
    private final SchoolClassService schoolClassService;
    private final SubjectService subjectService;
    private final StudentService studentService;
    private final AssignmentMapper assignmentMapper;

    /**
     * Infinite scroll: keyset-пагинация по id (от новых к старым), без подсчёта общего количества.
     *
     * @param cursor id последнего полученного задания; null — первая порция
     */
    public CursorPage<AssignmentResponse> scrollAssignments(Long cursor, int size) {
        Pageable pageable = PageRequest.of(0, Math.max(1, Math.min(size, com.lab.school.dto.common.PaginationConstants.MAX_PAGE_SIZE)));
        Slice<Assignment> slice = cursor == null
                ? assignmentRepository.findAllByOrderByIdDesc(pageable)
                : assignmentRepository.findByIdLessThanOrderByIdDesc(cursor, pageable);
        List<AssignmentResponse> items = slice.getContent().stream()
                .map(assignmentMapper::toResponse)
                .toList();
        Long nextCursor = slice.hasNext() && !items.isEmpty() ? items.get(items.size() - 1).id() : null;
        return new CursorPage<>(items, nextCursor, slice.hasNext());
    }

    /**
     * Классическая пагинация (общее количество отдаётся контроллером в заголовке X-Total-Count).
     */
    public Page<AssignmentResponse> listAssignmentsPage(int page, int size) {
        return assignmentRepository.findAll(PageRequests.byId(page, size))
                .map(assignmentMapper::toResponse);
    }

    public AssignmentResponse getAssignment(Long id) {
        return assignmentMapper.toResponse(requireAssignment(id));
    }

    @Transactional
    public AssignmentResponse createAssignment(AssignmentCreateRequest request) {
        Teacher teacher = teacherService.requireTeacher(request.teacherId());
        SchoolClass schoolClass = schoolClassService.requireSchoolClass(request.classId());
        Subject subject = subjectService.requireSubject(request.subjectId());
        // Учитель может задавать ДЗ только по своим предметам в своих классах
        if (!teacherService.existsWorkload(teacher.getId(), schoolClass.getId(), subject.getId())) {
            throw new BusinessException("Учитель id=" + teacher.getId() + " не ведёт предмет '"
                    + subject.getName() + "' в классе " + schoolClass.getName());
        }
        Assignment assignment = new Assignment();
        assignment.setTeacher(teacher);
        assignment.setSchoolClass(schoolClass);
        assignment.setSubject(subject);
        assignment.setTitle(request.title());
        assignment.setDescription(request.description());
        assignment.setDueDate(request.dueDate());
        return assignmentMapper.toResponse(assignmentRepository.save(assignment));
    }

    @Transactional
    public AssignmentResponse updateAssignment(Long id, AssignmentUpdateRequest request) {
        Assignment assignment = requireAssignment(id);
        assignment.setTitle(request.title());
        assignment.setDescription(request.description());
        assignment.setDueDate(request.dueDate());
        return assignmentMapper.toResponse(assignmentRepository.saveAndFlush(assignment));
    }

    @Transactional
    public void deleteAssignment(Long id) {
        assignmentRepository.delete(requireAssignment(id));
        assignmentRepository.flush();
    }

    @Transactional
    public SubmissionResponse submitSolution(Long assignmentId, SubmissionCreateRequest request) {
        Assignment assignment = requireAssignment(assignmentId);
        Student student = studentService.requireStudent(request.studentId());
        if (!Objects.equals(student.getSchoolClass().getId(), assignment.getSchoolClass().getId())) {
            throw new BusinessException("Ученик id=" + student.getId() + " не учится в классе "
                    + assignment.getSchoolClass().getName());
        }
        if (submissionRepository.existsByAssignmentIdAndStudentId(assignmentId, student.getId())) {
            throw new ConflictException("Ученик id=" + student.getId() + " уже отправил решение этого задания");
        }
        Submission submission = new Submission();
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setContentUrl(request.contentUrl());
        submission.setStatus(SubmissionStatus.SUBMITTED);
        return assignmentMapper.toSubmissionResponse(submissionRepository.save(submission));
    }

    public List<SubmissionResponse> listSubmissions(Long assignmentId) {
        requireAssignment(assignmentId);
        return submissionRepository.findByAssignmentIdOrderByIdAsc(assignmentId).stream()
                .map(assignmentMapper::toSubmissionResponse)
                .toList();
    }

    /**
     * Точка входа для других модулей: вернуть задание или 404.
     */
    public Assignment requireAssignment(Long id) {
        return assignmentRepository.findById(id).orElseThrow(() -> new NotFoundException("Домашнее задание", id));
    }

    /**
     * Решение конкретного ученика по заданию — нужно модулю оценок.
     */
    public Submission requireSubmission(Long assignmentId, Long studentId) {
        return submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElseThrow(() -> new NotFoundException("Решение ученика id=" + studentId
                        + " по заданию id=" + assignmentId + " не найдено"));
    }

    public List<Submission> collectGradedByStudents(Collection<Long> studentIds) {
        return submissionRepository.findByStudentIdInAndStatusOrderByGradedAtDesc(studentIds, SubmissionStatus.GRADED);
    }

    public List<Submission> collectGradedByTeacher(Long teacherId) {
        return submissionRepository.findByAssignment_Teacher_IdAndStatusOrderByGradedAtDesc(
                teacherId, SubmissionStatus.GRADED);
    }
}
