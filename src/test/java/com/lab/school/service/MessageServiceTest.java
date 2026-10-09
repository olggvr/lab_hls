package com.lab.school.service;

import com.lab.school.dto.message.BroadcastAudience;
import com.lab.school.dto.message.BroadcastRequest;
import com.lab.school.dto.message.BroadcastScope;
import com.lab.school.dto.message.MessageResponse;
import com.lab.school.entity.Message;
import com.lab.school.entity.Parent;
import com.lab.school.entity.ParentStudent;
import com.lab.school.entity.Student;
import com.lab.school.entity.User;
import com.lab.school.entity.enums.ParentRelation;
import com.lab.school.entity.enums.UserRole;
import com.lab.school.exception.BusinessException;
import com.lab.school.mapper.MessageMapper;
import com.lab.school.repository.MessageRecipientRepository;
import com.lab.school.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MessageRecipientRepository recipientRepository;

    @Mock
    private UserService userService;

    @Mock
    private StudentService studentService;

    @Mock
    private ParentStudentService parentStudentService;

    private MessageService messageService;

    @BeforeEach
    void setUp() {
        messageService = new MessageService(messageRepository, recipientRepository, userService,
                studentService, parentStudentService, Mappers.getMapper(MessageMapper.class));
    }

    @Test
    void broadcastToClass_includesStudentsAndParents_withoutDuplicates() {
        User teacher = user(1L, UserRole.TEACHER);
        Student s1 = student(10L, user(11L, UserRole.STUDENT));
        Student s2 = student(20L, user(21L, UserRole.STUDENT));
        Parent sharedParent = new Parent();
        sharedParent.setId(30L);
        sharedParent.setUser(user(31L, UserRole.PARENT));

        when(userService.requireUser(1L)).thenReturn(teacher);
        when(studentService.collectStudentsOfClass(5L)).thenReturn(List.of(s1, s2));
        when(parentStudentService.collectLinksOfStudents(anyCollection())).thenReturn(List.of(
                new ParentStudent(sharedParent, s1, ParentRelation.MOTHER),
                new ParentStudent(sharedParent, s2, ParentRelation.MOTHER)));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        MessageResponse response = messageService.broadcastMessage(new BroadcastRequest(
                1L, BroadcastScope.CLASS, 5L, null, BroadcastAudience.ALL, "Собрание", "Текст"));

        assertThat(response.recipientsCount()).isEqualTo(3);
        assertThat(response.senderId()).isEqualTo(1L);
    }

    @Test
    void broadcast_fromStudent_isRejected() {
        when(userService.requireUser(2L)).thenReturn(user(2L, UserRole.STUDENT));

        assertThatThrownBy(() -> messageService.broadcastMessage(new BroadcastRequest(
                2L, BroadcastScope.SCHOOL, null, null, BroadcastAudience.ALL, "s", "b")))
                .isInstanceOf(BusinessException.class);
        verify(messageRepository, never()).save(any());
    }

    @Test
    void broadcast_withoutRecipients_isRejected() {
        when(userService.requireUser(1L)).thenReturn(user(1L, UserRole.SUPERVISOR));
        when(studentService.collectStudentsOfParallel(3)).thenReturn(List.of());

        assertThatThrownBy(() -> messageService.broadcastMessage(new BroadcastRequest(
                1L, BroadcastScope.PARALLEL, null, 3, BroadcastAudience.STUDENTS, "s", "b")))
                .isInstanceOf(BusinessException.class);
        verify(messageRepository, never()).save(any());
    }

    @Test
    void broadcastToClass_withoutClassId_isRejected() {
        when(userService.requireUser(1L)).thenReturn(user(1L, UserRole.TEACHER));

        assertThatThrownBy(() -> messageService.broadcastMessage(new BroadcastRequest(
                1L, BroadcastScope.CLASS, null, null, BroadcastAudience.ALL, "s", "b")))
                .isInstanceOf(BusinessException.class);
        verify(messageRepository, never()).save(any());
    }

    private static User user(Long id, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setFullName("User " + id);
        return user;
    }

    private static Student student(Long id, User user) {
        Student student = new Student();
        student.setId(id);
        student.setUser(user);
        return student;
    }
}
