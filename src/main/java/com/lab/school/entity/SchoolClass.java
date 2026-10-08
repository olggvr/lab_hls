package com.lab.school.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "classes")
@Getter
public class SchoolClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter
    private Long id;

    @NotBlank
    @Size(max = 20)
    @Column(name = "name", nullable = false, unique = true, length = 20)
    @Setter
    private String name;

    /**
     * Параллель (год обучения): 1..11.
     */
    @NotNull
    @Min(1)
    @Max(11)
    @Column(name = "year", nullable = false)
    @Setter
    private Integer year;

    /**
     * One-to-Many: classes -> students.
     */
    @OneToMany(mappedBy = "schoolClass")
    private List<Student> students = new ArrayList<>();
}
