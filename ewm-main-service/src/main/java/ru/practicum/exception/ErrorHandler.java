package ru.practicum.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidation(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError == null
                ? e.getMessage()
                : String.format("Field: %s. Error: %s. Value: %s",
                fieldError.getField(), fieldError.getDefaultMessage(), fieldError.getRejectedValue());
        log.warn("400 BAD_REQUEST: {}", message);
        return build(HttpStatus.BAD_REQUEST, "Incorrectly made request.", message);
    }

    @ExceptionHandler({
            BadRequestException.class,
            ConstraintViolationException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            HandlerMethodValidationException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBadRequest(Exception e) {
        log.warn("400 BAD_REQUEST: {}", e.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Incorrectly made request.", e.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(NotFoundException e) {
        log.warn("404 NOT_FOUND: {}", e.getMessage());
        return build(HttpStatus.NOT_FOUND, "The required object was not found.", e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflict(ConflictException e) {
        log.warn("409 CONFLICT: {}", e.getMessage());
        return build(HttpStatus.CONFLICT, "For the requested operation the conditions are not met.", e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleIntegrity(DataIntegrityViolationException e) {
        log.warn("409 CONFLICT (DB): {}", e.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "Integrity constraint has been violated.",
                e.getMostSpecificCause().getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleOther(Exception e) {
        log.error("500 INTERNAL_SERVER_ERROR", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error.", e.getMessage());
    }

    private ApiError build(HttpStatus status, String reason, String message) {
        return ApiError.builder()
                .status(status.name())
                .reason(reason)
                .message(message)
                .timestamp(LocalDateTime.now())
                .errors(List.of())
                .build();
    }
}
