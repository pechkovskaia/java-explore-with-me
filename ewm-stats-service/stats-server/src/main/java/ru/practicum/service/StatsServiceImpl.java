package ru.practicum.service;

import jakarta.validation.ValidationException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.mapper.EndpointHitMapper;
import ru.practicum.model.EndpointHit;
import ru.practicum.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final StatsRepository statsRepository;

    @Override
    @Transactional
    public void saveHit(EndpointHitDto dto) {
        EndpointHit hit = EndpointHitMapper.toEntity(dto);
        statsRepository.save(hit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        if (start.isAfter(end)) {
            throw new ValidationException("Дата начала не может быть позже даты окончания");
        }

        if (uris == null || uris.isEmpty()) {
            if (unique) {
                return statsRepository.getUniqueStats(start, end);
            }
            return statsRepository.getAllStats(start, end);
        }

        if (unique) {
            return statsRepository.getUniqueStatsByUris(start, end, uris);
        }
        return statsRepository.getAllStatsByUris(start, end, uris);
    }
}
