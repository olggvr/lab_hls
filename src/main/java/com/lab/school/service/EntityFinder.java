package com.lab.school.service;

import com.lab.school.entity.Assignment;
import com.lab.school.entity.Parent;
import com.lab.school.entity.SchoolClass;
import com.lab.school.entity.Student;
import com.lab.school.entity.Subject;
import com.lab.school.entity.Teacher;
import com.lab.school.entity.User;
import com.lab.school.exception.NotFoundException;
import com.lab.school.repository.AssignmentRepository;
import com.lab.school.repository.ParentRepository;
import com.lab.school.repository.SchoolClassRepository;
import com.lab.school.repository.StudentRepository;
import com.lab.school.repository.SubjectRepository;
import com.lab.school.repository.TeacherRepository;
import com.lab.school.repository.UserRepository;
import org.springframework.stereotype.Component;

/**
 * Общие методы "найти или 404", чтобы не дублировать их в каждом сервисе.
 */
@Component
public class EntityFinder {

    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final AssignmentRepository assignmentRepository;

    public EntityFinder(UserRepository userRepository,
                        SubjectRepository subjectRepository,
                        SchoolClassRepository schoolClassRepository,
                        TeacherRepository teacherRepository,
                        StudentRepository studentRepository,
                        ParentRepository parentRepository,
                        AssignmentRepository assignmentRepository) {
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.parentRepository = parentRepository;
        this.assignmentRepository = assignmentRepository;
    }

    public User user(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("Пользователь", id));
    }

    public Subject subject(Long id) {
        return subjectRepository.findById(id).orElseThrow(() -> new NotFoundException("Предмет", id));
    }

    public SchoolClass schoolClass(Long id) {
        return schoolClassRepository.findById(id).orElseThrow(() -> new NotFoundException("Класс", id));
    }

    public Teacher teacher(Long id) {
        return teacherRepository.findById(id).orElseThrow(() -> new NotFoundException("Учитель", id));
    }

    public Student student(Long id) {
        return studentRepository.findById(id).orElseThrow(() -> new NotFoundException("Ученик", id));
    }

    public Parent parent(Long id) {
        return parentRepository.findById(id).orElseThrow(() -> new NotFoundException("Родитель", id));
    }

    public Assignment assignment(Long id) {
        return assignmentRepository.findById(id).orElseThrow(() -> new NotFoundException("Домашнее задание", id));
    }
}
