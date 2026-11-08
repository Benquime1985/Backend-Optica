package com.opticaguinto.optica.shared.pagination;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(List<T> content, long totalElements, int totalPages, int size, int number,
                               boolean first, boolean last, boolean empty, PageInfo page) {
    public record PageInfo(int size, int number, long totalElements, int totalPages) {}

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getTotalElements(), page.getTotalPages(),
                page.getSize(), page.getNumber(), page.isFirst(), page.isLast(), page.isEmpty(),
                new PageInfo(page.getSize(), page.getNumber(), page.getTotalElements(), page.getTotalPages()));
    }
}
