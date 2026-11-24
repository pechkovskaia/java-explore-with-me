package ru.practicum.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageUtil {

    private PageUtil() {
    }

    public static Pageable of(int from, int size) {
        return PageRequest.of(from / size, size);
    }

    public static Pageable of(int from, int size, Sort sort) {
        return PageRequest.of(from / size, size, sort);
    }
}