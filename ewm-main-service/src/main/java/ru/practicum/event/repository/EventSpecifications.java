package ru.practicum.event.repository;

import jakarta.persistence.criteria.Predicate;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class EventSpecifications {

    public static Specification<Event> adminFilter(List<Long> users,
                                                   List<EventState> states,
                                                   List<Long> categories,
                                                   LocalDateTime rangeStart,
                                                   LocalDateTime rangeEnd) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (users != null && !users.isEmpty()) {
                p.add(root.get("initiator").get("id").in(users));
            }
            if (states != null && !states.isEmpty()) {
                p.add(root.get("state").in(states));
            }
            if (categories != null && !categories.isEmpty()) {
                p.add(root.get("category").get("id").in(categories));
            }
            if (rangeStart != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("eventDate"), rangeStart));
            }
            if (rangeEnd != null) {
                p.add(cb.lessThanOrEqualTo(root.get("eventDate"), rangeEnd));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }
}