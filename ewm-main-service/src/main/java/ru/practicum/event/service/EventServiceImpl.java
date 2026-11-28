package ru.practicum.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.event.dto.*;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.AdminStateAction;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventSort;
import ru.practicum.event.model.EventState;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.event.repository.EventSpecifications;
import ru.practicum.exception.BadRequestException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;
import ru.practicum.util.PageUtil;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EventStatsService eventStats;

    @Override
    @Transactional
    public EventFullDto addEvent(Long userId, NewEventDto dto) {
        User user = getUser(userId);
        Category category = getCategory(dto.getCategory());
        checkEventDate(dto.getEventDate());

        Event saved = eventRepository.save(EventMapper.toEvent(dto, category, user));
        return EventMapper.toFullDto(saved, 0L, 0L); // у нового события счётчики всегда 0
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        getUser(userId);
        PageRequest page = PageRequest.of(from / size, size);
        return toShortList(eventRepository.findAllByInitiatorId(userId, page));
    }

    @Override
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        return toFull(getUserEventOrThrow(userId, eventId));
    }

    @Override
    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest dto) {
        Event event = getUserEventOrThrow(userId, eventId);

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }
        if (dto.getEventDate() != null) {
            checkEventDate(dto.getEventDate());
            event.setEventDate(dto.getEventDate());
        }
        if (dto.getAnnotation() != null) event.setAnnotation(dto.getAnnotation());
        if (dto.getCategory() != null) event.setCategory(getCategory(dto.getCategory()));
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (dto.getLocation() != null) event.setLocation(dto.getLocation());
        if (dto.getPaid() != null) event.setPaid(dto.getPaid());
        if (dto.getParticipantLimit() != null) event.setParticipantLimit(dto.getParticipantLimit());
        if (dto.getRequestModeration() != null) event.setRequestModeration(dto.getRequestModeration());
        if (dto.getTitle() != null) event.setTitle(dto.getTitle());

        if (dto.getStateAction() != null) {
            switch (dto.getStateAction()) {
                case SEND_TO_REVIEW -> event.setState(EventState.PENDING);
                case CANCEL_REVIEW -> event.setState(EventState.CANCELED);
            }
        }
        return toFull(event);
    }

    @Override
    public List<EventFullDto> searchByAdmin(List<Long> users, List<EventState> states, List<Long> categories,
                                            LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size) {
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new BadRequestException("rangeStart must be before rangeEnd");
        }
        return toFullList(eventRepository.findAll(
                EventSpecifications.adminFilter(users, states, categories, rangeStart, rangeEnd),
                PageUtil.of(from, size, Sort.by("id"))).getContent());
    }

    @Override
    @Transactional
    public EventFullDto updateByAdmin(Long eventId, UpdateEventAdminRequest dto) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (dto.getEventDate() != null && dto.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new BadRequestException("Event date must be at least 1 hour from now");
        }

        if (dto.getStateAction() == AdminStateAction.PUBLISH_EVENT) {
            if (event.getState() != EventState.PENDING) {
                throw new ConflictException("Cannot publish the event because it's not in the right state: "
                        + event.getState());
            }
            event.setState(EventState.PUBLISHED);
            event.setPublishedOn(LocalDateTime.now());
        } else if (dto.getStateAction() == AdminStateAction.REJECT_EVENT) {
            if (event.getState() == EventState.PUBLISHED) {
                throw new ConflictException("Cannot reject the event because it's already published");
            }
            event.setState(EventState.CANCELED);
        }

        if (dto.getAnnotation() != null) event.setAnnotation(dto.getAnnotation());
        if (dto.getCategory() != null) event.setCategory(getCategory(dto.getCategory()));
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (dto.getEventDate() != null) event.setEventDate(dto.getEventDate());
        if (dto.getLocation() != null) event.setLocation(dto.getLocation());
        if (dto.getPaid() != null) event.setPaid(dto.getPaid());
        if (dto.getParticipantLimit() != null) event.setParticipantLimit(dto.getParticipantLimit());
        if (dto.getRequestModeration() != null) event.setRequestModeration(dto.getRequestModeration());
        if (dto.getTitle() != null) event.setTitle(dto.getTitle());

        return toFull(event);
    }

    @Override
    public List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                               boolean onlyAvailable, EventSort sort, int from, int size,
                                               String uri, String ip) {
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new BadRequestException("rangeStart must be before rangeEnd");
        }
        if (rangeStart == null) {
            rangeStart = LocalDateTime.now(); // без диапазона показываем только будущие события
        }

        eventStats.saveHit(uri, ip);

        List<Event> events = eventRepository.findAll(
                EventSpecifications.publicFilter(text, categories, paid, rangeStart, rangeEnd),
                Sort.by("eventDate"));

        Map<Long, Long> confirmed = eventStats.getConfirmed(events);
        Map<Long, Long> views = eventStats.getViews(events);

        Stream<Event> stream = events.stream();
        if (onlyAvailable) {
            stream = stream.filter(e -> e.getParticipantLimit() == 0
                    || confirmed.getOrDefault(e.getId(), 0L) < e.getParticipantLimit());
        }

        Stream<EventShortDto> dtos = stream.map(e -> EventMapper.toShortDto(e,
                confirmed.getOrDefault(e.getId(), 0L),
                views.getOrDefault(e.getId(), 0L)));

        if (sort == EventSort.VIEWS) {
            dtos = dtos.sorted(Comparator.comparingLong(EventShortDto::getViews).reversed());
        }

        return dtos.skip(from).limit(size).toList();
    }

    @Override
    public EventFullDto getPublicEvent(Long id, String uri, String ip) {
        Event event = eventRepository.findById(id)
                .filter(e -> e.getState() == EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Event with id=" + id + " was not found"));

        eventStats.saveHit(uri, ip); // сначала сохраняем просмотр, чтобы он сразу попал в views
        return toFull(event);
    }

    private void checkEventDate(LocalDateTime eventDate) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Event date must be at least 2 hours from now: " + eventDate);
        }
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
    }

    private Category getCategory(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found"));
    }

    private Event getUserEventOrThrow(Long userId, Long eventId) {
        return eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
    }

    private EventFullDto toFull(Event event) {
        return toFullList(List.of(event)).get(0);
    }

    private List<EventFullDto> toFullList(List<Event> events) {
        Map<Long, Long> confirmed = eventStats.getConfirmed(events);
        Map<Long, Long> views = eventStats.getViews(events);
        return events.stream()
                .map(e -> EventMapper.toFullDto(e,
                        confirmed.getOrDefault(e.getId(), 0L),
                        views.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    private List<EventShortDto> toShortList(List<Event> events) {
        Map<Long, Long> confirmed = eventStats.getConfirmed(events);
        Map<Long, Long> views = eventStats.getViews(events);
        return events.stream()
                .map(e -> EventMapper.toShortDto(e,
                        confirmed.getOrDefault(e.getId(), 0L),
                        views.getOrDefault(e.getId(), 0L)))
                .toList();
    }
}