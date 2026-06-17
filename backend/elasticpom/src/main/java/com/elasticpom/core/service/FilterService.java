package com.elasticpom.core.service;

import com.elasticpom.adapters.FilterMapper;
import com.elasticpom.core.model.Filter;
import com.elasticpom.external.integration.FilterRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class FilterService {

    private final FilterRepository filterRepository;
    private final FilterMapper filterMapper;

    public FilterService(FilterRepository filterRepository, FilterMapper filterMapper) {
        this.filterRepository = filterRepository;
        this.filterMapper = filterMapper;
    }

    public List<Filter> getAllFilters() {
        return filterRepository.findAll().stream()
                .map(filterMapper::fromDocument)
                .sorted(Comparator.comparingInt(Filter::getOrder))
                .toList();
    }
}
