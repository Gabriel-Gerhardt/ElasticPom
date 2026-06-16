package com.elasticpom.core.service;

import com.elasticpom.core.model.FilterDefinition;
import com.elasticpom.core.model.FilterType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FilterService {
    private static final List<FilterDefinition> FILTERS = List.of(
        new FilterDefinition("subjects.keyword", 1, FilterType.OPTION),
        new FilterDefinition("contributors.keyword", 2, FilterType.OPTION),
        new FilterDefinition("date", 3, FilterType.RANGE)
    );

    public List<FilterDefinition> getAvailableFilters() {
        return FILTERS;
    }
}
