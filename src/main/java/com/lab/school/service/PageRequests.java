package com.lab.school.service;

import com.lab.school.dto.common.PaginationConstants;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

final class PageRequests {

    private PageRequests() {
    }

    /**
     * Страница, отсортированная по id. Размер страницы дополнительно ограничивается 50 записями —
     * даже если валидация на уровне контроллера будет обойдена.
     */
    static PageRequest byId(int page, int size) {
        int safeSize = Math.max(1, Math.min(size, PaginationConstants.MAX_PAGE_SIZE));
        return PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.ASC, "id"));
    }

    /**
     * Страница без сортировки — для запросов, где порядок задан в самом методе репозитория.
     */
    static PageRequest unsorted(int page, int size) {
        int safeSize = Math.max(1, Math.min(size, PaginationConstants.MAX_PAGE_SIZE));
        return PageRequest.of(Math.max(page, 0), safeSize);
    }
}
