package com.lab.school.dto.common;

public final class PaginationConstants {

    /** Максимальный размер страницы для любых findAll-эндпоинтов (по ТЗ — не более 50). */
    public static final int MAX_PAGE_SIZE = 50;
    public static final String DEFAULT_PAGE_SIZE = "20";
    public static final String DEFAULT_PAGE = "0";
    public static final String TOTAL_COUNT_HEADER = "X-Total-Count";

    private PaginationConstants() {
    }
}
