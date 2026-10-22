package com.lab.assignment.api;

import java.util.List;

public record CursorPage<T>(List<T> items, Long nextCursor, boolean hasNext) {
}
