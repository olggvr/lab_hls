package com.lab.school.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Many-to-Many с дополнительным полем: teachers &lt;-&gt; classes (+ subject, academic_year).
 */
@Entity
@Table(name = "teacher_class_subject")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeacherClassSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private SchoolClass schoolClass;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @NotNull
    @Pattern(regexp = "\\d{4}/\\d{4}")
    @Column(name = "academic_year", nullable = false, length = 9)
    private String academicYear;

    public TeacherClassSubject(Teacher teacher, SchoolClass schoolClass, Subject subject, String academicYear) {
        this.teacher = teacher;
        this.schoolClass = schoolClass;
        this.subject = subject;
        this.academicYear = academicYear;
    }
}
