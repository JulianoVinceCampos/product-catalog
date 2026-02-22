package com.catalog.dto.response;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

@Data @Builder
public class PageResponse<T> {
    private List<T> content;
    private int     page;
    private int     size;
    private long    totalElements;
    private int     totalPages;
    private boolean last;
    private boolean first;

    public static <S, T> PageResponse<T> from(Page<S> page, Function<S, T> mapper) {
        return PageResponse.<T>builder()
                .content(page.getContent().stream().map(mapper).toList())
                .page(page.getNumber()).size(page.getSize())
                .totalElements(page.getTotalElements()).totalPages(page.getTotalPages())
                .last(page.isLast()).first(page.isFirst()).build();
    }
}
