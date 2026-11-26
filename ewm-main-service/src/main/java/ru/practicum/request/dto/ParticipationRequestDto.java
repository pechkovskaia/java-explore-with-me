package ru.practicum.request.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.util.DateTimeUtils;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipationRequestDto {

    private Long id;

    @JsonFormat(pattern = DateTimeUtils.PATTERN)
    private LocalDateTime created;

    private Long event;

    private Long requester;

    private String status;
}