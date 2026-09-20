package dev.loganalyzer.controller;

import java.util.List;
import java.util.UUID;

import dev.loganalyzer.dto.SavedSearchRequest;
import dev.loganalyzer.dto.SavedSearchResponse;
import dev.loganalyzer.service.SavedSearchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/saved-searches")
public class SavedSearchController {
    private final SavedSearchService service;

    public SavedSearchController(SavedSearchService service) {
        this.service = service;
    }

    @GetMapping
    public List<SavedSearchResponse> findAll() {
        return service.findAll();
    }

    @PostMapping
    public ResponseEntity<SavedSearchResponse> create(@Valid @RequestBody SavedSearchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}