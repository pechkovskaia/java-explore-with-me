package ru.practicum.compilation.mapper;

import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.event.dto.EventShortDto;

import java.util.List;

public final class CompilationMapper {

    private CompilationMapper() {
    }

    public static CompilationDto toDto(Compilation c, List<EventShortDto> events) {
        return CompilationDto.builder()
                .id(c.getId())
                .events(events)
                .pinned(c.getPinned())
                .title(c.getTitle())
                .build();
    }
}