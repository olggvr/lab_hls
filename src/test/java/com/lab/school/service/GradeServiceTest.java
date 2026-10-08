package com.lab.school.service;

import com.lab.school.dto.assignment.GradeRequest;
import com.lab.school.dto.assignment.GradeResponse;
import com.lab.school.entity.Assignment;
import com.lab.school.entity.Message;
import com.lab.school.entity.MessageRecipient;
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
import com.lab.school.exception.NotFoundException;
import com.lab.school.mapper.AssignmentMapper;
import com.lab.school.repository.MessageRepository;
import com.lab.school.repository.ParentRepository;
import com.lab.school.repository.ParentStudentRepository;
import com.lab.school.repository.StudentRepository;
import com.lab.school.repository.SubmissionRepository;
import com.lab.school.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradeServiceTest {

    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private ParentStudentRepository parentStudentRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private ParentRepository parentRepository;
    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private EntityFinder finder;

    private GradeService gradeService;

    @BeforeEach
    void setUp() {
        gradeService = new GradeService(submissionRepository, parentStudentRepository, messageRepository,
                studentRepository, parentRepository, teacherRepository,
                Mappers.getMapper(AssignmentMapper.class), finder);
    }

    @Test
    void grade_updatesSubmission_andNotifiesStudentAndParentsInOneMessage() {
        Assignment assignment = assignment();
        Student student = student(10L, user(100L, "Ученик", UserRole.STUDENT));
        Submission submission = new Submission();
        submission.setId(1000L);
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setContentUrl("https://files/x.pdf");
        submission.setStatus(SubmissionStatus.SUBMITTED);

        when(finder.assignment(1L)).thenReturn(assignment);
        when(submissionRepository.findByAssignmentIdAndStudentId(1L, 10L)).thenReturn(Optional.of(submission));
        when(parentStudentRepository.findByStudentId(10L)).thenReturn(List.of(
                link(parent(user(201L, "Мама", UserRole.PARENT)), student, ParentRelation.MOTHER),
                link(parent(user(202L, "Папа", UserRole.PARENT)), student, ParentRelation.FATHER)));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        GradeResponse response = gradeService.grade(1L, new GradeRequest(10L, 4));

        assertThat(submission.getGrade()).isEqualTo(4);
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.GRADED);
        assertThat(submission.getGradedAt()).isNotNull();
        assertThat(response.notifiedRecipients()).isEqualTo(3);
        assertThat(response.submission().grade()).isEqualTo(4);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        Message message = captor.getValue();
        assertThat(message.getType()).isEqualTo(MessageType.GRADE_NOTIFICATION);
        assertThat(message.getSender().getId()).isEqualTo(50L);
        assertThat(message.getRecipients())
                .extracting(MessageRecipient::getUser)
                .extracting(User::getId)
                .containsExactly(100L, 201L, 202L);
    }

    @Test
    void grade_withoutSubmission_throwsNotFound_andSendsNothing() {
        when(finder.assignment(1L)).thenReturn(assignment());
        when(submissionRepository.findByAssignmentIdAndStudentId(1L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gradeService.grade(1L, new GradeRequest(10L, 5)))
                .isInstanceOf(NotFoundException.class);
        verify(messageRepository, never()).save(any());
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
