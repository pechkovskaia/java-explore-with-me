package ru.practicum.compilation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;
import ru.practicum.compilation.mapper.CompilationMapper;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.compilation.repository.CompilationRepository;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.event.service.EventStatsService;
import ru.practicum.exception.NotFoundException;
import ru.practicum.util.PageUtil;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final EventStatsService eventStats;

    @Override
    @Transactional
    public CompilationDto addCompilation(NewCompilationDto dto) {
        Compilation compilation = new Compilation();
        compilation.setTitle(dto.getTitle());
        compilation.setPinned(dto.getPinned() != null ? dto.getPinned() : false);
        compilation.setEvents(getEvents(dto.getEvents()));

        return toDtos(List.of(compilationRepository.save(compilation))).get(0);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest dto) {
        Compilation compilation = getOrThrow(compId);

        if (dto.getTitle() != null) compilation.setTitle(dto.getTitle());
        if (dto.getPinned() != null) compilation.setPinned(dto.getPinned());
        if (dto.getEvents() != null) compilation.setEvents(getEvents(dto.getEvents()));

        return toDtos(List.of(compilation)).get(0);
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Compilation with id=" + compId + " was not found");
        }
        compilationRepository.deleteById(compId);
    }

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        Pageable page = PageUtil.of(from, size, Sort.by("id"));
        List<Compilation> compilations = pinned == null
                ? compilationRepository.findAll(page).getContent()
                : compilationRepository.findAllByPinned(pinned, page);
        return toDtos(compilations);
    }

    @Override
    public CompilationDto getCompilation(Long compId) {
        return toDtos(List.of(getOrThrow(compId))).get(0);
    }

    // ===================== HELPERS =====================

    private Compilation getOrThrow(Long compId) {
        return compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));
    }

    private Set<Event> getEvents(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashSet<>();
        }
        List<Event> events = eventRepository.findAllById(ids);
        if (events.size() != ids.size()) {
            throw new NotFoundException("Some events from the compilation were not found");
        }
        return new HashSet<>(events);
    }

    // Счётчики для всех событий всех подборок считаются за один проход
    private List<CompilationDto> toDtos(List<Compilation> compilations) {
        List<Event> allEvents = compilations.stream()
                .flatMap(c -> c.getEvents().stream())
                .distinct()
                .toList();

        Map<Long, Long> confirmed = eventStats.getConfirmed(allEvents);
        Map<Long, Long> views = eventStats.getViews(allEvents);

        return compilations.stream()
                .map(c -> {
                    List<EventShortDto> events = c.getEvents().stream()
                            .sorted(Comparator.comparing(Event::getId))
                            .map(e -> EventMapper.toShortDto(e,
                                    confirmed.getOrDefault(e.getId(), 0L),
                                    views.getOrDefault(e.getId(), 0L)))
                            .toList();
                    return CompilationMapper.toDto(c, events);
                })
                .toList();
    }
}
