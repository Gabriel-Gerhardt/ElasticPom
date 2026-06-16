package com.elasticpom.external.rest;

import com.elasticpom.adapters.PaperMapper;
import com.elasticpom.core.model.Paper;
import com.elasticpom.core.service.PaperService;
import com.elasticpom.exception.BadRequestException;
import com.elasticpom.exception.InvalidFilterException;
import com.elasticpom.adapters.dto.request.FilterRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaperController.class)
class PaperControllerFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaperService paperService;

    @MockitoBean
    private PaperMapper paperMapper;

    // -------------------------------------------------------------------------
    // Filters field is deserialized from snake_case JSON
    // -------------------------------------------------------------------------

    @Test
    void searchByQuery_withFilters_deserializesSnakeCaseCorrectly() throws Exception {
        Paper paper = new Paper();
        paper.setPaperId("p1");
        when(paperService.getPapersByQuery(anyString(), anyInt(), anyInt(), anyList()))
                .thenReturn(List.of(paper));
        when(paperMapper.toDto(any())).thenReturn(null);

        String requestBody = """
                {
                  "query": "deep learning",
                  "pageSize": 10,
                  "page": 0,
                  "filters": [
                    {"filter_name": "language", "filter_option": "en"}
                  ]
                }
                """;

        mockMvc.perform(post("/api/papers/search-by-query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        ArgumentCaptor<List<FilterRequest>> filtersCaptor = ArgumentCaptor.forClass(List.class);
        verify(paperService).getPapersByQuery(eq("deep learning"), eq(10), eq(0), filtersCaptor.capture());

        List<FilterRequest> capturedFilters = filtersCaptor.getValue();
        assertThat(capturedFilters).hasSize(1);
        assertThat(capturedFilters.get(0).filterName()).isEqualTo("language");
        assertThat(capturedFilters.get(0).filterOption()).isEqualTo("en");
    }

    // -------------------------------------------------------------------------
    // No filters in body → service receives null
    // -------------------------------------------------------------------------

    @Test
    void searchByQuery_withoutFilters_passesNullToService() throws Exception {
        Paper paper = new Paper();
        paper.setPaperId("p2");
        when(paperService.getPapersByQuery(anyString(), anyInt(), anyInt(), isNull()))
                .thenReturn(List.of(paper));
        when(paperMapper.toDto(any())).thenReturn(null);

        String requestBody = """
                {
                  "query": "machine learning",
                  "pageSize": 10,
                  "page": 0
                }
                """;

        mockMvc.perform(post("/api/papers/search-by-query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        verify(paperService).getPapersByQuery(eq("machine learning"), eq(10), eq(0), isNull());
    }

    // -------------------------------------------------------------------------
    // Multiple filters are deserialized correctly
    // -------------------------------------------------------------------------

    @Test
    void searchByQuery_withMultipleFilters_deserializesAll() throws Exception {
        when(paperService.getPapersByQuery(anyString(), anyInt(), anyInt(), anyList()))
                .thenReturn(List.of());

        String requestBody = """
                {
                  "query": "neural networks",
                  "pageSize": 10,
                  "page": 0,
                  "filters": [
                    {"filter_name": "language", "filter_option": "en"},
                    {"filter_name": "year", "filter_option": "2023"}
                  ]
                }
                """;

        // Service returns empty list → PaperNotInElasticException → 404
        // but we only care that filters were correctly passed; 404 is acceptable here
        mockMvc.perform(post("/api/papers/search-by-query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        ArgumentCaptor<List<FilterRequest>> filtersCaptor = ArgumentCaptor.forClass(List.class);
        verify(paperService).getPapersByQuery(eq("neural networks"), eq(10), eq(0), filtersCaptor.capture());

        List<FilterRequest> filters = filtersCaptor.getValue();
        assertThat(filters).hasSize(2);
        assertThat(filters).extracting(FilterRequest::filterName)
                .containsExactly("language", "year");
        assertThat(filters).extracting(FilterRequest::filterOption)
                .containsExactly("en", "2023");
    }

    // -------------------------------------------------------------------------
    // InvalidFilterException → 404
    // -------------------------------------------------------------------------

    @Test
    void searchByQuery_invalidFilter_returns404() throws Exception {
        when(paperService.getPapersByQuery(anyString(), anyInt(), anyInt(), anyList()))
                .thenThrow(new InvalidFilterException("Filter 'bad_field' does not exist in the index mapping"));

        String requestBody = """
                {
                  "query": "test",
                  "pageSize": 10,
                  "page": 0,
                  "filters": [
                    {"filter_name": "bad_field", "filter_option": "val"}
                  ]
                }
                """;

        mockMvc.perform(post("/api/papers/search-by-query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // BadRequestException (malformed numeric) → 400
    // -------------------------------------------------------------------------

    @Test
    void searchByQuery_malformedNumericFilter_returns400() throws Exception {
        when(paperService.getPapersByQuery(anyString(), anyInt(), anyInt(), anyList()))
                .thenThrow(new BadRequestException("Invalid value 'abc' for integer field 'year'"));

        String requestBody = """
                {
                  "query": "test",
                  "pageSize": 10,
                  "page": 0,
                  "filters": [
                    {"filter_name": "year", "filter_option": "abc"}
                  ]
                }
                """;

        mockMvc.perform(post("/api/papers/search-by-query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }
}
