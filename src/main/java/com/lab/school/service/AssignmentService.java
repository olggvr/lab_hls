package com.lab.school.service;

import com.lab.school.dto.assignment.AssignmentCreateRequest;
import com.lab.school.dto.assignment.AssignmentResponse;
import com.lab.school.dto.assignment.AssignmentUpdateRequest;
import com.lab.school.dto.assignment.SubmissionCreateRequest;
import com.lab.school.dto.assignment.SubmissionResponse;
import com.lab.school.dto.common.CursorPage;
import com.lab.school.dto.common.PaginationConstants;
import com.lab.school.entity.Assignment;
import com.lab.school.entity.SchoolClass;
import com.lab.school.entity.Student;
import com.lab.school.entity.Subject;
import com.lab.school.entity.Submission;
import com.lab.school.entity.Teacher;
import com.lab.school.entity.enums.SubmissionStatus;
import com.lab.school.exception.BusinessException;
import com.lab.school.exception.ConflictException;
import com.lab.school.mapper.AssignmentMapper;
import com.lab.school.repository.AssignmentRepository;
import com.lab.school.repository.SubmissionRepository;
import com.lab.school.repository.TeacherClassSubjectRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final TeacherClassSubjectRepository workloadRepository;
    private final AssignmentMapper assignmentMapper;
    private final EntityFinder finder;

    public AssignmentService(AssignmentRepository assignmentRepository,
                             SubmissionRepository submissionRepository,
                             TeacherClassSubjectRepository workloadRepository,
                             AssignmentMapper assignmentMapper,
                             EntityFinder finder) {
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.workloadRepository = workloadRepository;
        this.assignmentMapper = assignmentMapper;
        this.finder = finder;
    }

    /**
     * Infinite scroll: keyset-пагинация по id (от новых к старым), без подсчёта общего количества.
     *
     * @param cursor id последнего полученного задания; null — первая порция
     */
    public CursorPage<AssignmentResponse> scroll(Long cursor, int size) {
        int safeSize = Math.max(1, Math.min(size, PaginationConstants.MAX_PAGE_SIZE));
        Pageable pageable = PageRequest.of(0, safeSize);
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
    public Page<AssignmentResponse> findPage(int page, int size) {
        return assignmentRepository.findAll(PageRequests.byId(page, size)).map(assignmentMapper::toResponse);
    }

    public AssignmentResponse getById(Long id) {
        return assignmentMapper.toResponse(finder.assignment(id));
    }

    @Transactional
    public AssignmentResponse create(AssignmentCreateRequest request) {
        Teacher teacher = finder.teacher(request.teacherId());
        SchoolClass schoolClass = finder.schoolClass(request.classId());
        Subject subject = finder.subject(request.subjectId());
        // Учитель может задавать ДЗ только по своим предметам в своих классах
        if (!workloadRepository.existsByTeacherIdAndSchoolClassIdAndSubjectId(
                teacher.getId(), schoolClass.getId(), subject.getId())) {
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
    public AssignmentResponse update(Long id, AssignmentUpdateRequest request) {
        Assignment assignment = finder.assignment(id);
        assignment.setTitle(request.title());
        assignment.setDescription(request.description());
        assignment.setDueDate(request.dueDate());
        return assignmentMapper.toResponse(assignmentRepository.saveAndFlush(assignment));
    }

    @Transactional
    public void delete(Long id) {
        assignmentRepository.delete(finder.assignment(id));
        assignmentRepository.flush();
    }

    @Transactional
    public SubmissionResponse submit(Long assignmentId, SubmissionCreateRequest request) {
        Assignment assignment = finder.assignment(assignmentId);
        Student student = finder.student(request.studentId());
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

    public List<SubmissionResponse> getSubmissions(Long assignmentId) {
        finder.assignment(assignmentId);
        return submissionRepository.findByAssignmentIdOrderByIdAsc(assignmentId).stream()
                .map(assignmentMapper::toSubmissionResponse)
                .toList();
    }
}
