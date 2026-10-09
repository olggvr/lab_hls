package com.lab.school.service;

import com.lab.school.dto.assignment.GradeRequest;
import com.lab.school.dto.assignment.GradeResponse;
import com.lab.school.entity.Assignment;
import com.lab.school.entity.Message;
import com.lab.school.entity.Parent;
import com.lab.school.entity.ParentStudent;
import com.lab.school.entity.Student;
import com.lab.school.entity.Subject;
import com.lab.school.entity.Submission;
import com.lab.school.entity.Teacher;
import com.lab.school.entity.User;
import com.lab.school.entity.enums.MessageType;
import com.lab.school.entity.enums.ParentRelation;
import com.lab.school.entity.enums.SubmissionStatus;
import com.lab.school.entity.enums.UserRole;
import com.lab.school.exception.BusinessException;
import com.lab.school.exception.NotFoundException;
import com.lab.school.mapper.AssignmentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradeServiceTest {

    @Mock
    private AssignmentService assignmentService;

    @Mock
    private StudentService studentService;

    @Mock
    private ParentService parentService;

    @Mock
    private ParentStudentService parentStudentService;

    @Mock
    private TeacherService teacherService;

    @Mock
    private MessageService messageService;

    @Mock
    private UserService userService;

    private GradeService gradeService;

    @BeforeEach
    void setUp() {
        gradeService = new GradeService(assignmentService, studentService, parentService, parentStudentService,
                teacherService, messageService, userService, Mappers.getMapper(AssignmentMapper.class));
    }

    @Test
    void gradeSubmission_updatesSubmission_andNotifiesStudentAndParentsInOneMessage() {
        Assignment assignment = assignment();
        Student student = student(10L, user(100L, "Ученик", UserRole.STUDENT));
        Submission submission = new Submission();
        submission.setId(1000L);
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setContentUrl("https://files/x.pdf");
        submission.setStatus(SubmissionStatus.SUBMITTED);

        when(assignmentService.requireAssignment(1L)).thenReturn(assignment);
        when(assignmentService.requireSubmission(1L, 10L)).thenReturn(submission);
        when(parentStudentService.collectLinksOfStudent(10L)).thenReturn(List.of(
                link(parent(user(201L, "Мама", UserRole.PARENT)), student, ParentRelation.MOTHER),
                link(parent(user(202L, "Папа", UserRole.PARENT)), student, ParentRelation.FATHER)));
        when(messageService.notifyAboutGrade(any(User.class), anyString(), anyString(), anyCollection()))
                .thenAnswer(invocation -> buildNotification(invocation.getArgument(0), invocation.getArgument(1),
                        invocation.getArgument(2), invocation.getArgument(3)));

        GradeResponse response = gradeService.gradeSubmission(1L, new GradeRequest(10L, 4));

        assertThat(submission.getGrade()).isEqualTo(4);
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.GRADED);
        assertThat(submission.getGradedAt()).isNotNull();
        assertThat(response.notifiedRecipients()).isEqualTo(3);
        assertThat(response.submission().grade()).isEqualTo(4);

        ArgumentCaptor<User> senderCaptor = ArgumentCaptor.forClass(User.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<User>> recipientsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(messageService).notifyAboutGrade(senderCaptor.capture(), anyString(), anyString(),
                recipientsCaptor.capture());
        assertThat(senderCaptor.getValue().getId()).isEqualTo(50L);
        assertThat(recipientsCaptor.getValue())
                .extracting(User::getId)
                .containsExactly(100L, 201L, 202L);
    }

    @Test
    void gradeSubmission_withoutSubmission_throwsNotFound_andSendsNothing() {
        when(assignmentService.requireAssignment(1L)).thenReturn(assignment());
        when(assignmentService.requireSubmission(1L, 10L))
                .thenThrow(new NotFoundException("Решение ученика id=10 по заданию id=1 не найдено"));

        assertThatThrownBy(() -> gradeService.gradeSubmission(1L, new GradeRequest(10L, 5)))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(messageService);
    }

    @Test
    void listGradesOfUser_forSupervisor_isRejected() {
        when(userService.requireUser(1L)).thenReturn(user(1L, "Админ", UserRole.SUPERVISOR));

        assertThatThrownBy(() -> gradeService.listGradesOfUser(1L))
                .isInstanceOf(BusinessException.class);
    }

    private static Message buildNotification(User sender, String subject, String body, Collection<User> recipients) {
        Message message = new Message(sender, subject, body, MessageType.GRADE_NOTIFICATION);
        recipients.forEach(message::addRecipient);
        return message;
    }

    private static Assignment assignment() {
        Teacher teacher = new Teacher();
        teacher.setId(5L);
        teacher.setUser(user(50L, "Учитель", UserRole.TEACHER));
        Subject subject = new Subject();
        subject.setId(7L);
        subject.setName("Математика");
        Assignment assignment = new Assignment();
        assignment.setId(1L);
        assignment.setTeacher(teacher);
        assignment.setSubject(subject);
        assignment.setTitle("Уравнения");
        return assignment;
    }

    private static User user(Long id, String name, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setFullName(name);
        user.setRole(role);
        return user;
    }

    private static Student student(Long id, User user) {
        Student student = new Student();
        student.setId(id);
        student.setUser(user);
        return student;
    }

    private static Parent parent(User user) {
        Parent parent = new Parent();
        parent.setId(user.getId());
        parent.setUser(user);
        return parent;
    }

    private static ParentStudent link(Parent parent, Student student, ParentRelation relation) {
        return new ParentStudent(parent, student, relation);
    }
}
