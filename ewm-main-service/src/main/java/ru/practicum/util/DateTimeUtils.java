package ru.practicum.util;

import java.time.format.DateTimeFormatter;

public final class DateTimeUtils {

    public static final String PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(PATTERN);

    private DateTimeUtils() {
    }
}
