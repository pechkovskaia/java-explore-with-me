package ru.practicum.event.service;

import ru.practicum.event.dto.*;
import ru.practicum.event.model.EventState;

import java.time.LocalDateTime;
import java.util.List;

public interface EventService {

    EventFullDto addEvent(Long userId, NewEventDto dto);

    List<EventShortDto> getUserEvents(Long userId, int from, int size);

    EventFullDto getUserEvent(Long userId, Long eventId);

    EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest dto);

    List<EventFullDto> searchByAdmin(List<Long> users, List<EventState> states, List<Long> categories,
                                     LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size);

    EventFullDto updateByAdmin(Long eventId, UpdateEventAdminRequest dto);
}
