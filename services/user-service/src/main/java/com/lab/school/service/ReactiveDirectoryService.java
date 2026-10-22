package com.lab.school.service;

import com.lab.school.dto.message.BroadcastAudience;
import com.lab.school.entity.ParentStudent;
import com.lab.school.entity.SchoolClass;
import com.lab.school.entity.Student;
import com.lab.school.entity.Subject;
import com.lab.school.entity.Teacher;
import com.lab.school.entity.User;
import com.lab.school.entity.enums.UserRole;
import com.lab.school.exception.BusinessException;
import com.lab.school.integration.AudienceResponse;
import com.lab.school.integration.AssignmentReferenceRequest;
import com.lab.school.integration.AssignmentReferenceResponse;
import com.lab.school.integration.BroadcastAudienceRequest;
import com.lab.school.integration.DirectAudienceRequest;
import com.lab.school.integration.GradeAudienceResponse;
import com.lab.school.integration.GradeScopeResponse;
import com.lab.school.integration.StudentReferenceRequest;
import com.lab.school.integration.StudentReferenceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reactive boundary over Spring Data JPA. Blocking repository work is explicitly
 * moved to boundedElastic, so Reactor threads are never blocked by JDBC.
 */
@Service
@RequiredArgsConstructor
public class ReactiveDirectoryService {
    private final UserService userService;
    private final StudentService studentService;
    private final ParentStudentService parentStudentService;
    private final ParentService parentService;
    private final TeacherService teacherService;
    private final SchoolClassService schoolClassService;
    private final SubjectService subjectService;

    public Mono<AudienceResponse> resolveDirect(DirectAudienceRequest request) {
        return Mono.fromCallable(() -> {
            User sender = userService.requireUser(request.senderId());
            List<Long> ids = userService.requireUsers(request.recipientIds()).stream().map(User::getId).toList();
            return new AudienceResponse(sender.getId(), sender.getFullName(), ids);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<AudienceResponse> resolveBroadcast(BroadcastAudienceRequest request) {
        return Mono.fromCallable(() -> resolveBroadcastBlocking(request))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<AssignmentReferenceResponse> resolveAssignment(AssignmentReferenceRequest request) {
        return Mono.fromCallable(() -> {
            Teacher teacher = teacherService.requireTeacher(request.teacherId());
            SchoolClass schoolClass = schoolClassService.requireSchoolClass(request.classId());
            Subject subject = subjectService.requireSubject(request.subjectId());
            if (!teacherService.existsWorkload(teacher.getId(), schoolClass.getId(), subject.getId())) {
                throw new BusinessException("Учитель id=" + teacher.getId() + " не ведёт предмет '"
                        + subject.getName() + "' в классе " + schoolClass.getName());
            }
            return new AssignmentReferenceResponse(
                    teacher.getId(), teacher.getUser().getId(), teacher.getUser().getFullName(),
                    schoolClass.getId(), schoolClass.getName(), subject.getId(), subject.getName());
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<StudentReferenceResponse> resolveStudent(StudentReferenceRequest request) {
        return Mono.fromCallable(() -> {
            Student student = studentService.requireStudent(request.studentId());
            if (!student.getSchoolClass().getId().equals(request.classId())) {
                throw new BusinessException("Ученик id=" + student.getId() + " не учится в классе id="
                        + request.classId());
            }
            return new StudentReferenceResponse(student.getId(), student.getUser().getId(),
                    student.getUser().getFullName(), student.getSchoolClass().getId());
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<GradeAudienceResponse> resolveGradeAudience(Long studentId) {
        return Mono.fromCallable(() -> {
            Student student = studentService.requireStudent(studentId);
            Map<Long, User> recipients = new LinkedHashMap<>();
            recipients.put(student.getUser().getId(), student.getUser());
            for (ParentStudent link : parentStudentService.collectLinksOfStudent(studentId)) {
                User parent = link.getParent().getUser();
                recipients.put(parent.getId(), parent);
            }
            return new GradeAudienceResponse(studentId, student.getUser().getFullName(),
                    List.copyOf(recipients.keySet()));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<GradeScopeResponse> resolveGradeScope(Long userId) {
        return Mono.fromCallable(() -> {
            User user = userService.requireUser(userId);
            return switch (user.getRole()) {
                case STUDENT -> new GradeScopeResponse(user.getRole().name(),
                        studentService.findStudentByUserId(userId).map(s -> List.of(s.getId())).orElseGet(List::of), null);
                case PARENT -> new GradeScopeResponse(user.getRole().name(),
                        parentService.findParentByUserId(userId)
                                .map(parent -> parentStudentService.collectChildIdsOfParent(parent.getId()))
                                .orElseGet(List::of), null);
                case TEACHER -> new GradeScopeResponse(user.getRole().name(), List.of(),
                        teacherService.findTeacherByUserId(userId).map(Teacher::getId).orElse(null));
                case SUPERVISOR -> throw new BusinessException("У администратора нет оценок");
            };
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private AudienceResponse resolveBroadcastBlocking(BroadcastAudienceRequest request) {
        User sender = userService.requireUser(request.senderId());
        if (sender.getRole() != UserRole.TEACHER && sender.getRole() != UserRole.SUPERVISOR) {
            throw new BusinessException("Массовую рассылку может делать только учитель или администратор");
        }
        List<Student> students = switch (request.scope()) {
            case CLASS -> {
                if (request.classId() == null) throw new BusinessException("Для рассылки по классу нужен classId");
                yield studentService.collectStudentsOfClass(request.classId());
            }
            case PARALLEL -> {
                if (request.year() == null) throw new BusinessException("Для рассылки по параллели нужен year");
                yield studentService.collectStudentsOfParallel(request.year());
            }
            case SCHOOL -> studentService.collectAllStudents();
        };
        Map<Long, User> recipients = new LinkedHashMap<>();
        if (request.audience() == BroadcastAudience.STUDENTS || request.audience() == BroadcastAudience.ALL) {
            students.forEach(student -> recipients.put(student.getUser().getId(), student.getUser()));
        }
        if (request.audience() == BroadcastAudience.PARENTS || request.audience() == BroadcastAudience.ALL) {
            List<Long> studentIds = students.stream().map(Student::getId).toList();
            if (!studentIds.isEmpty()) {
                for (ParentStudent link : parentStudentService.collectLinksOfStudents(studentIds)) {
                    User parent = link.getParent().getUser();
                    recipients.put(parent.getId(), parent);
                }
            }
        }
        return new AudienceResponse(sender.getId(), sender.getFullName(), List.copyOf(recipients.keySet()));
    }
}
