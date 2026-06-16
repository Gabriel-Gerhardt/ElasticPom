package com.elasticpom.external.rest;

import com.elasticpom.adapters.PaperMapper;
import com.elasticpom.core.model.FilterDefinition;
import com.elasticpom.core.model.FilterType;
import com.elasticpom.core.service.FilterService;
import com.elasticpom.core.service.PaperService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaperController.class)
class FilterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaperService paperService;

    @MockitoBean
    private PaperMapper paperMapper;

    @MockitoBean
    private FilterService filterService;

    @Test
    void getFilters_returns200WithFilterList() throws Exception {
        when(filterService.getAvailableFilters()).thenReturn(List.of(
            new FilterDefinition("subjects.keyword", 1, FilterType.OPTION),
            new FilterDefinition("contributors.keyword", 2, FilterType.OPTION),
            new FilterDefinition("date", 3, FilterType.RANGE)
        ));

        mockMvc.perform(get("/api/papers/filters").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].filtername").value("subjects.keyword"))
            .andExpect(jsonPath("$[0].order").value(1))
            .andExpect(jsonPath("$[0].type").value("option"))
            .andExpect(jsonPath("$[1].filtername").value("contributors.keyword"))
            .andExpect(jsonPath("$[1].order").value(2))
            .andExpect(jsonPath("$[1].type").value("option"))
            .andExpect(jsonPath("$[2].filtername").value("date"))
            .andExpect(jsonPath("$[2].order").value(3))
            .andExpect(jsonPath("$[2].type").value("range"));
    }

    @Test
    void getFilters_returnsEmptyListWhenNoFilters() throws Exception {
        when(filterService.getAvailableFilters()).thenReturn(List.of());

        mockMvc.perform(get("/api/papers/filters").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }
}
