package ru.practicum.event.mapper;

import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.model.Category;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.user.mapper.UserMapper;
import ru.practicum.user.model.User;

import java.time.LocalDateTime;

public final class EventMapper {

    private EventMapper() {
    }

    public static Event toEvent(NewEventDto dto, Category category, User initiator) {
        Event event = new Event();
        event.setAnnotation(dto.getAnnotation());
        event.setCategory(category);
        event.setDescription(dto.getDescription());
        event.setEventDate(dto.getEventDate());
        event.setInitiator(initiator);
        event.setLocation(dto.getLocation());
        event.setPaid(dto.getPaid() != null ? dto.getPaid() : false);
        event.setParticipantLimit(dto.getParticipantLimit() != null ? dto.getParticipantLimit() : 0);
        event.setRequestModeration(dto.getRequestModeration() != null ? dto.getRequestModeration() : true);
        event.setTitle(dto.getTitle());
        event.setCreatedOn(LocalDateTime.now());
        event.setState(EventState.PENDING);
        return event;
    }

    public static EventFullDto toFullDto(Event e, long confirmedRequests, long views) {
        return EventFullDto.builder()
                .id(e.getId())
                .annotation(e.getAnnotation())
                .category(CategoryMapper.toCategoryDto(e.getCategory()))
                .confirmedRequests(confirmedRequests)
                .createdOn(e.getCreatedOn())
                .description(e.getDescription())
                .eventDate(e.getEventDate())
                .initiator(UserMapper.toUserShortDto(e.getInitiator()))
                .location(e.getLocation())
                .paid(e.getPaid())
                .participantLimit(e.getParticipantLimit())
                .publishedOn(e.getPublishedOn())
                .requestModeration(e.getRequestModeration())
                .state(e.getState())
                .title(e.getTitle())
                .views(views)
                .build();
    }

    public static EventShortDto toShortDto(Event e, long confirmedRequests, long views) {
        return EventShortDto.builder()
                .id(e.getId())
                .annotation(e.getAnnotation())
                .category(CategoryMapper.toCategoryDto(e.getCategory()))
                .confirmedRequests(confirmedRequests)
                .eventDate(e.getEventDate())
                .initiator(UserMapper.toUserShortDto(e.getInitiator()))
                .paid(e.getPaid())
                .title(e.getTitle())
                .views(views)
                .build();
    }
}
