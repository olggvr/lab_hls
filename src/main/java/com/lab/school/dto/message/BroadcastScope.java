package com.lab.school.dto.message;

public enum BroadcastScope {
    /** Один класс (нужен classId). */
    CLASS,
    /** Параллель — все классы одного года обучения (нужен year). */
    PARALLEL,
    /** Вся школа. */
    SCHOOL
}
