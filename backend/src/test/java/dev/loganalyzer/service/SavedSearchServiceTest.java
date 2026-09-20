package dev.loganalyzer.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import dev.loganalyzer.dto.SavedSearchRequest;
import dev.loganalyzer.dto.SearchSortDirection;
import dev.loganalyzer.entity.SavedSearch;
import dev.loganalyzer.entity.Severity;
import dev.loganalyzer.repository.SavedSearchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SavedSearchServiceTest {
    @Test
    void storesAndListsTypedSearchDefinition() {
        SavedSearchRepository repository = mock(SavedSearchRepository.class);
        SavedSearchService service = new SavedSearchService(repository);
        SavedSearchRequest request = request("Production errors");
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var created = service.create(request);
        when(repository.findAllByOrderByNameAsc()).thenReturn(List.of(new SavedSearch(request, Instant.now())));

        assertThat(created.name()).isEqualTo("Production errors");
        assertThat(created.severity()).isEqualTo(Severity.ERROR);
        assertThat(service.findAll()).singleElement().extracting("query").isEqualTo("payment timeout");
    }

    @Test
    void rejectsInvalidTimeRange() {
        SavedSearchService service = new SavedSearchService(mock(SavedSearchRepository.class));
        SavedSearchRequest request = new SavedSearchRequest("Invalid", null, null, null, null, null,
                Instant.parse("2026-09-19T12:00:00Z"), Instant.parse("2026-09-19T11:00:00Z"),
                SearchSortDirection.NEWEST);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("startTimestamp must be before");
    }

    @Test
    void rejectsDeletingUnknownSearch() {
        SavedSearchRepository repository = mock(SavedSearchRepository.class);
        SavedSearchService service = new SavedSearchService(repository);
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
        verify(repository).existsById(id);
    }

    private SavedSearchRequest request(String name) {
        return new SavedSearchRequest(name, "payment timeout", Severity.ERROR, "payment-service", "production",
                "trace-42", null, null, SearchSortDirection.NEWEST);
    }
}