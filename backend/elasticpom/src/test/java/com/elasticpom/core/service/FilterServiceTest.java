package com.elasticpom.core.service;

import com.elasticpom.core.model.FilterDefinition;
import com.elasticpom.core.model.FilterType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FilterServiceTest {

    private final FilterService filterService = new FilterService();

    @Test
    void getAvailableFilters_returnsThreeFilters() {
        List<FilterDefinition> filters = filterService.getAvailableFilters();
        assertThat(filters).hasSize(3);
    }

    @Test
    void getAvailableFilters_subjectsKeywordIsOptionAtOrder1() {
        FilterDefinition subjects = filterService.getAvailableFilters().get(0);
        assertThat(subjects.filtername()).isEqualTo("subjects.keyword");
        assertThat(subjects.order()).isEqualTo(1);
        assertThat(subjects.type()).isEqualTo(FilterType.OPTION);
    }

    @Test
    void getAvailableFilters_contributorsKeywordIsOptionAtOrder2() {
        FilterDefinition contributors = filterService.getAvailableFilters().get(1);
        assertThat(contributors.filtername()).isEqualTo("contributors.keyword");
        assertThat(contributors.order()).isEqualTo(2);
        assertThat(contributors.type()).isEqualTo(FilterType.OPTION);
    }

    @Test
    void getAvailableFilters_dateIsRangeAtOrder3() {
        FilterDefinition date = filterService.getAvailableFilters().get(2);
        assertThat(date.filtername()).isEqualTo("date");
        assertThat(date.order()).isEqualTo(3);
        assertThat(date.type()).isEqualTo(FilterType.RANGE);
    }

    @Test
    void getAvailableFilters_returnsImmutableList() {
        List<FilterDefinition> first = filterService.getAvailableFilters();
        List<FilterDefinition> second = filterService.getAvailableFilters();
        assertThat(first).isSameAs(second);
    }
}
