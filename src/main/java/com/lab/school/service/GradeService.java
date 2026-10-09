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
import com.lab.school.entity.enums.SubmissionStatus;
import com.lab.school.exception.BusinessException;
import com.lab.school.mapper.AssignmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Модуль оценок. Своих таблиц не имеет: работает через модули заданий, людей и сообщений.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GradeService {

    private final AssignmentService assignmentService;
    private final StudentService studentService;
    private final ParentService parentService;
    private final ParentStudentService parentStudentService;
    private final TeacherService teacherService;
    private final MessageService messageService;
    private final UserService userService;
    private final AssignmentMapper assignmentMapper;

    /**
     * ТРАНЗАКЦИЯ №1: выставление оценки + уведомление ученика и его родителей.
     * <p>
     * Обновление submissions и вставка в messages / message_recipients выполняются атомарно:
     * если уведомление не удалось сохранить, оценка тоже откатывается — родитель никогда не
     * получит уведомление без оценки, а оценка не появится "молча" без уведомления.
     */
    @Transactional
    public GradeResponse gradeSubmission(Long assignmentId, GradeRequest request) {
        Assignment assignment = assignmentService.requireAssignment(assignmentId);
        Submission submission = assignmentService.requireSubmission(assignmentId, request.studentId());

        submission.setGrade(request.grade());
        submission.setStatus(SubmissionStatus.GRADED);
        submission.setGradedAt(LocalDateTime.now());

        Student student = submission.getStudent();
        List<User> recipients = new ArrayList<>();
        recipients.add(student.getUser());
        for (ParentStudent link : parentStudentService.collectLinksOfStudent(student.getId())) {
            recipients.add(link.getParent().getUser());
        }

        Message notification = messageService.notifyAboutGrade(
                assignment.getTeacher().getUser(),
                "Новая оценка: " + assignment.getSubject().getName(),
                "Ученик " + student.getUser().getFullName() + " получил оценку " + request.grade()
                        + " за задание «" + assignment.getTitle() + "».",
                recipients);

        return new GradeResponse(
                assignmentMapper.toSubmissionResponse(submission),
                notification.getId(),
                notification.getRecipients().size());
    }

    /**
     * Оценки пользователя. Логика зависит от роли пользователя:
     * ученик — свои оценки, родитель — оценки детей, учитель — оценки по его заданиям.
     */
    public List<GradeEntryResponse> listGradesOfUser(Long userId) {
        User user = userService.requireUser(userId);
        List<Submission> submissions = switch (user.getRole()) {
            case STUDENT -> studentService.findStudentByUserId(userId)
                    .map(student -> assignmentService.collectGradedByStudents(List.of(student.getId())))
                    .orElseGet(List::of);
            case PARENT -> parentService.findParentByUserId(userId)
                    .map(parent -> parentStudentService.collectChildIdsOfParent(parent.getId()))
                    .filter(childIds -> !childIds.isEmpty())
                    .map(assignmentService::collectGradedByStudents)
                    .orElseGet(List::of);
            case TEACHER -> teacherService.findTeacherByUserId(userId)
                    .map(teacher -> assignmentService.collectGradedByTeacher(teacher.getId()))
                    .orElseGet(List::of);
            case SUPERVISOR -> throw new BusinessException("У администратора нет оценок");
        };
        return submissions.stream().map(assignmentMapper::toGradeEntry).toList();
    }
}
