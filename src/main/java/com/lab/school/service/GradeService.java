package com.lab.school.service;

import com.lab.school.dto.assignment.GradeRequest;
import com.lab.school.dto.assignment.GradeResponse;
import com.lab.school.dto.user.GradeEntryResponse;
import com.lab.school.entity.Assignment;
import com.lab.school.entity.Message;
import com.lab.school.entity.ParentStudent;
import com.lab.school.entity.Student;
import com.lab.school.entity.Submission;
import com.lab.school.entity.User;
import com.lab.school.entity.enums.MessageType;
import com.lab.school.entity.enums.SubmissionStatus;
import com.lab.school.exception.BusinessException;
import com.lab.school.exception.NotFoundException;
import com.lab.school.mapper.AssignmentMapper;
import com.lab.school.repository.MessageRepository;
import com.lab.school.repository.ParentRepository;
import com.lab.school.repository.ParentStudentRepository;
import com.lab.school.repository.StudentRepository;
import com.lab.school.repository.SubmissionRepository;
import com.lab.school.repository.TeacherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class GradeService {

    private final SubmissionRepository submissionRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final MessageRepository messageRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final TeacherRepository teacherRepository;
    private final AssignmentMapper assignmentMapper;
    private final EntityFinder finder;

    public GradeService(SubmissionRepository submissionRepository,
                        ParentStudentRepository parentStudentRepository,
                        MessageRepository messageRepository,
                        StudentRepository studentRepository,
                        ParentRepository parentRepository,
                        TeacherRepository teacherRepository,
                        AssignmentMapper assignmentMapper,
                        EntityFinder finder) {
        this.submissionRepository = submissionRepository;
        this.parentStudentRepository = parentStudentRepository;
        this.messageRepository = messageRepository;
        this.studentRepository = studentRepository;
        this.parentRepository = parentRepository;
        this.teacherRepository = teacherRepository;
        this.assignmentMapper = assignmentMapper;
        this.finder = finder;
    }

    /**
     * ТРАНЗАКЦИЯ №1: выставление оценки + уведомление ученика и его родителей.
     * <p>
     * Обновление submissions и вставка в messages / message_recipients выполняются атомарно:
     * если уведомление не удалось сохранить, оценка тоже откатывается — родитель никогда не
     * получит уведомление без оценки, а оценка не появится "молча" без уведомления.
     */
    @Transactional
    public GradeResponse grade(Long assignmentId, GradeRequest request) {
        Assignment assignment = finder.assignment(assignmentId);
        Submission submission = submissionRepository.findByAssignmentIdAndStudentId(assignmentId, request.studentId())
                .orElseThrow(() -> new NotFoundException("Решение ученика id=" + request.studentId()
                        + " по заданию id=" + assignmentId + " не найдено"));

        submission.setGrade(request.grade());
        submission.setStatus(SubmissionStatus.GRADED);
        submission.setGradedAt(LocalDateTime.now());

        Student student = submission.getStudent();
        User teacherUser = assignment.getTeacher().getUser();
        Message notification = new Message(
                teacherUser,
                "Новая оценка: " + assignment.getSubject().getName(),
                "Ученик " + student.getUser().getFullName() + " получил оценку " + request.grade()
                        + " за задание «" + assignment.getTitle() + "».",
                MessageType.GRADE_NOTIFICATION);
        notification.addRecipient(student.getUser());
        for (ParentStudent link : parentStudentRepository.findByStudentId(student.getId())) {
            notification.addRecipient(link.getParent().getUser());
        }
        messageRepository.save(notification);

        return new GradeResponse(
                assignmentMapper.toSubmissionResponse(submission),
                notification.getId(),
                notification.getRecipients().size());
    }

    /**
     * Оценки пользователя. Логика зависит от роли пользователя:
     * ученик — свои оценки, родитель — оценки детей, учитель — оценки по его заданиям.
     */
    public List<GradeEntryResponse> getGradesForUser(Long userId) {
        User user = finder.user(userId);
        List<Submission> submissions = switch (user.getRole()) {
            case STUDENT -> studentRepository.findByUserId(userId)
                    .map(s -> submissionRepository.findByStudentIdInAndStatusOrderByGradedAtDesc(
                            List.of(s.getId()), SubmissionStatus.GRADED))
                    .orElse(List.of());
            case PARENT -> parentRepository.findByUserId(userId)
                    .map(p -> parentStudentRepository.findByParentId(p.getId()).stream()
                            .map(link -> link.getStudent().getId())
                            .toList())
                    .filter(ids -> !ids.isEmpty())
                    .map(ids -> submissionRepository.findByStudentIdInAndStatusOrderByGradedAtDesc(
                            ids, SubmissionStatus.GRADED))
                    .orElse(List.of());
            case TEACHER -> teacherRepository.findByUserId(userId)
                    .map(t -> submissionRepository.findByAssignment_Teacher_IdAndStatusOrderByGradedAtDesc(
                            t.getId(), SubmissionStatus.GRADED))
                    .orElse(List.of());
            case SUPERVISOR -> throw new BusinessException("У администратора нет оценок");
        };
        return submissions.stream().map(assignmentMapper::toGradeEntry).toList();
    }
}
