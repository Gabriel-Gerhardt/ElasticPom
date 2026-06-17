package com.elasticpom.external.rest;

import com.elasticpom.adapters.FilterMapper;
import com.elasticpom.adapters.dto.FilterDto;
import com.elasticpom.core.model.Filter;
import com.elasticpom.core.service.FilterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FilterController.class)
class FilterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FilterService filterService;

    @MockitoBean
    private FilterMapper filterMapper;

    @Test
    void getFilters_returns200WithFilterList() throws Exception {
        Filter filter1 = new Filter("subjects.keyword",     1, "option");
        Filter filter2 = new Filter("contributors.keyword", 2, "option");
        Filter filter3 = new Filter("date",                 3, "range");

        when(filterService.getAllFilters()).thenReturn(List.of(filter1, filter2, filter3));
        when(filterMapper.toDto(filter1)).thenReturn(new FilterDto("subjects.keyword",     1, "option"));
        when(filterMapper.toDto(filter2)).thenReturn(new FilterDto("contributors.keyword", 2, "option"));
        when(filterMapper.toDto(filter3)).thenReturn(new FilterDto("date",                 3, "range"));

        mockMvc.perform(get("/api/filters").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].filtername").value("subjects.keyword"))
                .andExpect(jsonPath("$[0].order").value(1))
                .andExpect(jsonPath("$[0].type").value("option"))
                .andExpect(jsonPath("$[1].filtername").value("contributors.keyword"))
                .andExpect(jsonPath("$[2].filtername").value("date"))
                .andExpect(jsonPath("$[2].type").value("range"));
    }

    @Test
    void getFilters_returnsEmptyList_whenNoFilters() throws Exception {
        when(filterService.getAllFilters()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/filters").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
