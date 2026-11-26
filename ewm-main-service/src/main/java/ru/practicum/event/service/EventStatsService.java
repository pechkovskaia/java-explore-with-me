package ru.practicum.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.client.StatsClient;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.event.model.Event;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.request.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventStatsService {

    private static final String APP = "ewm-main-service";
    private static final String EVENT_URI = "/events/";

    private final StatsClient statsClient;
    private final RequestRepository requestRepository;

    public void saveHit(String uri, String ip) {
        try {
            EndpointHitDto hit = new EndpointHitDto();
            hit.setApp(APP);
            hit.setUri(uri);
            hit.setIp(ip);
            hit.setTimestamp(LocalDateTime.now());
            statsClient.hit(hit);
        } catch (Exception e) {
            log.warn("Не удалось сохранить обращение в статистику: {}", e.getMessage());
        }
    }

    public Map<Long, Long> getConfirmed(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = events.stream().map(Event::getId).toList();
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : requestRepository.countByEventIdsAndStatus(ids, RequestStatus.CONFIRMED)) {
            result.put((Long) row[0], (Long) row[1]);
        }
        return result;
    }

    public Map<Long, Long> getViews(List<Event> events) {
        LocalDateTime start = events.stream()
                .map(Event::getPublishedOn)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
        if (start == null) {
            return Map.of(); // неопубликованные события никто не смотрел
        }
        List<String> uris = events.stream().map(e -> EVENT_URI + e.getId()).toList();
        try {
            List<ViewStatsDto> stats = statsClient.getStats(
                    start.minusSeconds(1), LocalDateTime.now().plusSeconds(1), uris, true);
            Map<Long, Long> result = new HashMap<>();
            for (ViewStatsDto s : stats) {
                Long id = Long.parseLong(s.getUri().substring(EVENT_URI.length()));
                result.put(id, s.getHits());
            }
            return result;
        } catch (Exception e) {
            log.warn("Не удалось получить статистику: {}", e.getMessage());
            return Map.of();
        }
    }
}