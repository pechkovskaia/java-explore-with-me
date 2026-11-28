package ru.practicum.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import ru.practicum.util.DateTimeUtils;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ApiError {

    private List<String> errors;
    private String status;
    private String reason;
    private String message;

    @JsonFormat(pattern = DateTimeUtils.PATTERN)
    private LocalDateTime timestamp;
}