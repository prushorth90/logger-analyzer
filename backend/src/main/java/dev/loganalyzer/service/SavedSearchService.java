package dev.loganalyzer.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import dev.loganalyzer.dto.SavedSearchRequest;
import dev.loganalyzer.dto.SavedSearchResponse;
import dev.loganalyzer.entity.SavedSearch;
import dev.loganalyzer.repository.SavedSearchRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SavedSearchService {
    private final SavedSearchRepository repository;

    public SavedSearchService(SavedSearchRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<SavedSearchResponse> findAll() {
        return repository.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public SavedSearchResponse create(SavedSearchRequest request) {
        if (request.startTimestamp() != null && request.endTimestamp() != null
                && !request.startTimestamp().isBefore(request.endTimestamp())) {
            throw new IllegalArgumentException("startTimestamp must be before endTimestamp");
        }
        try {
            return toResponse(repository.saveAndFlush(new SavedSearch(request, Instant.now())));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A saved search with this name already exists");
        }
    }

    @Transactional
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Saved search not found");
        }
        repository.deleteById(id);
    }

    private SavedSearchResponse toResponse(SavedSearch savedSearch) {
        return new SavedSearchResponse(savedSearch.getId(), savedSearch.getName(), savedSearch.getQuery(),
                savedSearch.getSeverity(), savedSearch.getServiceName(), savedSearch.getEnvironment(),
                savedSearch.getTraceId(), savedSearch.getStartTimestamp(), savedSearch.getEndTimestamp(),
                savedSearch.getSortDirection(), savedSearch.getCreatedAt());
    }
}