package com.elasticpom.external.rest;

import com.elasticpom.adapters.FilterMapper;
import com.elasticpom.adapters.dto.FilterDto;
import com.elasticpom.core.service.FilterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/filters")
public class FilterController {

    private final FilterService filterService;
    private final FilterMapper filterMapper;

    public FilterController(FilterService filterService, FilterMapper filterMapper) {
        this.filterService = filterService;
        this.filterMapper = filterMapper;
    }

    @GetMapping
    public ResponseEntity<List<FilterDto>> getFilters() {
        List<FilterDto> filters = filterService.getAllFilters().stream()
                .map(filterMapper::toDto)
                .toList();
        return ResponseEntity.ok(filters);
    }
}
