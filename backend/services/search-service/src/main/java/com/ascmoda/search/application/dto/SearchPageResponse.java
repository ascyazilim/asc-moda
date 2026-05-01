package com.ascmoda.search.application.dto;

import java.util.List;

public record SearchPageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
