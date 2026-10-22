package com.lab.school.service;

import com.lab.school.dto.message.BroadcastAudience;
import com.lab.school.entity.ParentStudent;
import com.lab.school.entity.Student;
import com.lab.school.entity.User;
import com.lab.school.entity.enums.UserRole;
import com.lab.school.exception.BusinessException;
import com.lab.school.integration.AudienceResponse;
import com.lab.school.integration.BroadcastAudienceRequest;
import com.lab.school.integration.DirectAudienceRequest;
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
