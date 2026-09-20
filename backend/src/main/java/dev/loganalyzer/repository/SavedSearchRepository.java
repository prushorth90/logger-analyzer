package dev.loganalyzer.repository;

import java.util.List;
import java.util.UUID;

import dev.loganalyzer.entity.SavedSearch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedSearchRepository extends JpaRepository<SavedSearch, UUID> {
    List<SavedSearch> findAllByOrderByNameAsc();
}