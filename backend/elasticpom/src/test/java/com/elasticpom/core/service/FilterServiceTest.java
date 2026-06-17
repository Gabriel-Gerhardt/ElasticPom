package com.elasticpom.core.service;

import com.elasticpom.adapters.FilterMapper;
import com.elasticpom.core.model.Filter;
import com.elasticpom.external.document.FilterDocument;
import com.elasticpom.external.integration.FilterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilterServiceTest {

    @Mock
    private FilterRepository filterRepository;

    @Mock
    private FilterMapper filterMapper;

    private FilterService filterService;

    @BeforeEach
    void setUp() {
        filterService = new FilterService(filterRepository, filterMapper);
    }

    @Test
    void getAllFilters_returnsEmptyList_whenRepositoryIsEmpty() {
        when(filterRepository.findAll()).thenReturn(Collections.emptyList());

        List<Filter> result = filterService.getAllFilters();

        assertThat(result).isEmpty();
    }

    @Test
    void getAllFilters_sortsByOrder() {
        FilterDocument docDate         = new FilterDocument("date",                 3, "range");
        FilterDocument docSubjects     = new FilterDocument("subjects.keyword",     1, "option");
        FilterDocument docContributors = new FilterDocument("contributors.keyword", 2, "option");

        Filter fDate         = new Filter("date",                 3, "range");
        Filter fSubjects     = new Filter("subjects.keyword",     1, "option");
        Filter fContributors = new Filter("contributors.keyword", 2, "option");

        // Repository returns in arbitrary order
        when(filterRepository.findAll()).thenReturn(List.of(docDate, docSubjects, docContributors));
        when(filterMapper.fromDocument(docDate)).thenReturn(fDate);
        when(filterMapper.fromDocument(docSubjects)).thenReturn(fSubjects);
        when(filterMapper.fromDocument(docContributors)).thenReturn(fContributors);

        List<Filter> result = filterService.getAllFilters();

        assertThat(result).extracting(Filter::getOrder).containsExactly(1, 2, 3);
        assertThat(result).extracting(Filter::getFiltername)
                .containsExactly("subjects.keyword", "contributors.keyword", "date");
    }

    @Test
    void getAllFilters_mapsDocumentsToDomain() {
        FilterDocument doc = new FilterDocument("subjects.keyword", 1, "option");
        Filter filter = new Filter("subjects.keyword", 1, "option");

        when(filterRepository.findAll()).thenReturn(List.of(doc));
        when(filterMapper.fromDocument(doc)).thenReturn(filter);

        List<Filter> result = filterService.getAllFilters();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFiltername()).isEqualTo("subjects.keyword");
        assertThat(result.get(0).getOrder()).isEqualTo(1);
        assertThat(result.get(0).getType()).isEqualTo("option");
    }
}
