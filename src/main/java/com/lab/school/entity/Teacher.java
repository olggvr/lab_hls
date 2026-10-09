package com.lab.school.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "teachers")
@Getter
@Setter
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * Основной предмет учителя (может быть не задан).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    /**
     * One-to-Many: teachers -&gt; assignments.
     */
    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "teacher")
    private List<Assignment> assignments = new ArrayList<>();

    /**
     * Нагрузка учителя: классы и предметы по учебным годам (M:N с доп. полем).
     */
    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "teacher")
    private List<TeacherClassSubject> workload = new ArrayList<>();
}
