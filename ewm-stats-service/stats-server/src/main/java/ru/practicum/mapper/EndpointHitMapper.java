package ru.practicum.mapper;

import ru.practicum.dto.EndpointHitDto;
import ru.practicum.model.EndpointHit;

public class EndpointHitMapper {

    private EndpointHitMapper() {
    }

    public static EndpointHit toEntity(EndpointHitDto dto) {
        return EndpointHit.builder()
                .app(dto.getApp())
                .uri(dto.getUri())
                .ip(dto.getIp())
                .timestamp(dto.getTimestamp())
                .build();
    }
}
