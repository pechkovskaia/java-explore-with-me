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
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public EventFullDto addEvent(Long userId, NewEventDto dto) {
        User user = getUser(userId);
        Category category = getCategory(dto.getCategory());
        checkEventDate(dto.getEventDate());

        Event saved = eventRepository.save(EventMapper.toEvent(dto, category, user));
        return EventMapper.toFullDto(saved, 0L, 0L);
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        getUser(userId);
        PageRequest page = PageRequest.of(from / size, size);
        // TODO: confirmedRequests — после заявок, views — после статистики
        return eventRepository.findAllByInitiatorId(userId, page).stream()
                .map(e -> EventMapper.toShortDto(e, 0L, 0L))
                .toList();
    }

    @Override
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        Event event = getUserEventOrThrow(userId, eventId);
        return EventMapper.toFullDto(event, 0L, 0L);
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
        if (dto.getAnnotation() != null) {
            event.setAnnotation(dto.getAnnotation());
        }
        if (dto.getCategory() != null) {
            event.setCategory(getCategory(dto.getCategory()));
        }
        if (dto.getDescription() != null) {
            event.setDescription(dto.getDescription());
        }
        if (dto.getLocation() != null) {
            event.setLocation(dto.getLocation());
        }
        if (dto.getPaid() != null) {
            event.setPaid(dto.getPaid());
        }
        if (dto.getParticipantLimit() != null) {
            event.setParticipantLimit(dto.getParticipantLimit());
        }
        if (dto.getRequestModeration() != null) {
            event.setRequestModeration(dto.getRequestModeration());
        }
        if (dto.getTitle() != null) {
            event.setTitle(dto.getTitle());
        }
        if (dto.getStateAction() != null) {
            switch (dto.getStateAction()) {
                case SEND_TO_REVIEW -> event.setState(EventState.PENDING);
                case CANCEL_REVIEW -> event.setState(EventState.CANCELED);
            }
        }
        return EventMapper.toFullDto(event, 0L, 0L);
    }

    private void checkEventDate(LocalDateTime eventDate) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException(
                    "Event date must be at least 2 hours from now: " + eventDate);
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

    @Override
    public List<EventFullDto> searchByAdmin(List<Long> users, List<EventState> states, List<Long> categories,
                                            LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size) {
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new BadRequestException("rangeStart must be before rangeEnd");
        }
        return eventRepository.findAll(
                        EventSpecifications.adminFilter(users, states, categories, rangeStart, rangeEnd),
                        PageUtil.of(from, size, Sort.by("id")))
                .stream()
                .map(e -> EventMapper.toFullDto(e, 0, 0))
                .toList();
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
                throw new ConflictException("Cannot publish the event because it's not in the right state: " + event.getState());
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
        if (dto.getCategory() != null) event.setCategory(getCategoryOrThrow(dto.getCategory()));
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (dto.getEventDate() != null) event.setEventDate(dto.getEventDate());
        if (dto.getLocation() != null) event.setLocation(dto.getLocation());
        if (dto.getPaid() != null) event.setPaid(dto.getPaid());
        if (dto.getParticipantLimit() != null) event.setParticipantLimit(dto.getParticipantLimit());
        if (dto.getRequestModeration() != null) event.setRequestModeration(dto.getRequestModeration());
        if (dto.getTitle() != null) event.setTitle(dto.getTitle());

        return EventMapper.toFullDto(event, 0, 0);
    }

    private Category getCategoryOrThrow(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found"));
    }
}