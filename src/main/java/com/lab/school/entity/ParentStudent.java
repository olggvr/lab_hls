package com.lab.school.entity;

import com.lab.school.entity.enums.ParentRelation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

/**
 * Many-to-Many с дополнительным полем: parents <-> students (+ relation).
 */
@Entity
@Table(name = "parent_student")
public class ParentStudent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_id", nullable = false)
    private Parent parent;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "relation", nullable = false, length = 20)
    private ParentRelation relation;

    protected ParentStudent() {
    }

    public ParentStudent(Parent parent, Student student, ParentRelation relation) {
        this.parent = parent;
        this.student = student;
        this.relation = relation;
    }

    public Long getId() {
        return id;
    }

    public Parent getParent() {
        return parent;
    }

    public Student getStudent() {
        return student;
    }

    public ParentRelation getRelation() {
        return relation;
    }

    public void setRelation(ParentRelation relation) {
        this.relation = relation;
    }
}
