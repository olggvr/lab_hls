package com.lab.school.service;

import com.lab.school.entity.Parent;
import com.lab.school.entity.ParentStudent;
import com.lab.school.entity.SchoolClass;
import com.lab.school.entity.Student;
import com.lab.school.entity.Subject;
import com.lab.school.entity.Teacher;
import com.lab.school.entity.User;
import com.lab.school.entity.enums.ParentRelation;
import com.lab.school.entity.enums.UserRole;
import com.lab.school.integration.AssignmentReferenceRequest;
import com.lab.school.integration.DirectAudienceRequest;
import com.lab.school.integration.StudentReferenceRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactiveDirectoryServiceTest {
    @Mock UserService userService;
    @Mock StudentService studentService;
    @Mock ParentStudentService parentStudentService;
    @Mock ParentService parentService;
    @Mock TeacherService teacherService;
    @Mock SchoolClassService schoolClassService;
    @Mock SubjectService subjectService;

    private ReactiveDirectoryService service;
    private User teacherUser;
    private Student student;
    private Teacher teacher;
    private SchoolClass schoolClass;
    private Subject subject;

    @BeforeEach
    void setUp() {
        service = new ReactiveDirectoryService(userService, studentService, parentStudentService,
                parentService, teacherService, schoolClassService, subjectService);

        teacherUser = user(2L, "Teacher", UserRole.TEACHER);
        teacher = new Teacher();
        teacher.setId(10L);
        teacher.setUser(teacherUser);
        schoolClass = new SchoolClass();
        schoolClass.setId(20L);
        schoolClass.setName("7A");
        subject = new Subject();
        subject.setId(30L);
        subject.setName("Math");
        student = new Student();
        student.setId(40L);
        student.setUser(user(4L, "Student", UserRole.STUDENT));
        student.setSchoolClass(schoolClass);
    }

    @Test
    void resolvesDirectAssignmentStudentAndGradeAudience() {
        User parentUser = user(7L, "Parent", UserRole.PARENT);
        Parent parent = new Parent();
        parent.setId(50L);
        parent.setUser(parentUser);

        when(userService.requireUser(2L)).thenReturn(teacherUser);
        when(userService.requireUsers(Set.of(4L, 7L))).thenReturn(List.of(student.getUser(), parentUser));
        when(teacherService.requireTeacher(10L)).thenReturn(teacher);
        when(schoolClassService.requireSchoolClass(20L)).thenReturn(schoolClass);
        when(subjectService.requireSubject(30L)).thenReturn(subject);
        when(teacherService.existsWorkload(10L, 20L, 30L)).thenReturn(true);
        when(studentService.requireStudent(40L)).thenReturn(student);
        when(parentStudentService.collectLinksOfStudent(40L))
                .thenReturn(List.of(new ParentStudent(parent, student, ParentRelation.MOTHER)));

        StepVerifier.create(service.resolveDirect(new DirectAudienceRequest(2L, Set.of(4L, 7L))))
                .assertNext(result -> assertThat(result.recipientIds()).containsExactly(4L, 7L))
                .verifyComplete();
        StepVerifier.create(service.resolveAssignment(new AssignmentReferenceRequest(10L, 20L, 30L)))
                .assertNext(result -> assertThat(result.teacherUserId()).isEqualTo(2L))
                .verifyComplete();
        StepVerifier.create(service.resolveStudent(new StudentReferenceRequest(40L, 20L)))
                .assertNext(result -> assertThat(result.studentUserId()).isEqualTo(4L))
                .verifyComplete();
        StepVerifier.create(service.resolveGradeAudience(40L))
                .assertNext(result -> assertThat(result.recipientIds()).containsExactly(4L, 7L))
                .verifyComplete();
    }

    @Test
    void resolvesStudentParentAndTeacherGradeScopes() {
        User studentUser = student.getUser();
        User parentUser = user(7L, "Parent", UserRole.PARENT);

        when(userService.requireUser(4L)).thenReturn(studentUser);
        when(studentService.findStudentByUserId(4L)).thenReturn(java.util.Optional.of(student));
        when(userService.requireUser(7L)).thenReturn(parentUser);
        Parent parent = new Parent();
        parent.setId(50L);
        when(parentService.findParentByUserId(7L)).thenReturn(java.util.Optional.of(parent));
        when(parentStudentService.collectChildIdsOfParent(50L)).thenReturn(List.of(40L));
        when(userService.requireUser(2L)).thenReturn(teacherUser);
        when(teacherService.findTeacherByUserId(2L)).thenReturn(java.util.Optional.of(teacher));

        StepVerifier.create(service.resolveGradeScope(4L))
                .assertNext(scope -> assertThat(scope.studentIds()).containsExactly(40L)).verifyComplete();
        StepVerifier.create(service.resolveGradeScope(7L))
                .assertNext(scope -> assertThat(scope.studentIds()).containsExactly(40L)).verifyComplete();
        StepVerifier.create(service.resolveGradeScope(2L))
                .assertNext(scope -> assertThat(scope.teacherId()).isEqualTo(10L)).verifyComplete();
    }

    private static User user(Long id, String name, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setFullName(name);
        user.setRole(role);
        return user;
    }
}
