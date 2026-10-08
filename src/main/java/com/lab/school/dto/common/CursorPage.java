package com.lab.school.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Страница ленты для infinite scroll (без общего количества записей)")
public record CursorPage<T>(
        List<T> items,
        @Schema(description = "Курсор для следующего запроса; null, если записей больше нет")
        Long nextCursor,
        boolean hasNext
) {
}
