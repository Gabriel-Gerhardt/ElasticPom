package com.elasticpom.external.rest;

import com.elasticpom.adapters.PaperMapper;
import com.elasticpom.core.model.Paper;
import com.elasticpom.core.service.PaperService;
import com.elasticpom.exception.PaperNotInElasticException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaperController.class)
class PaperControllerHybridSearchTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaperService paperService;

    @MockitoBean
    private PaperMapper paperMapper;

    // -------------------------------------------------------------------------
    // Valid request (text query only - no queryVector) -> 200
    // -------------------------------------------------------------------------

    @Test
    void hybridSearch_validRequest_returns200() throws Exception {
        Paper paper = new Paper();
        paper.setPaperId("p1");
        when(paperService.getPapersByHybridSearch(anyString(), anyInt(), anyInt(), isNull()))
                .thenReturn(List.of(paper));
        when(paperMapper.toDto(any())).thenReturn(null);

        String requestBody = """
                {
                  "query": "deep learning",
                  "pageSize": 10,
                  "page": 0
                }
                """;

        mockMvc.perform(post("/api/papers/hybrid-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // Valid request with filters -> 200, filters passed through to the service
    // -------------------------------------------------------------------------

    @Test
    void hybridSearch_withFilters_passesFiltersToService() throws Exception {
        Paper paper = new Paper();
        paper.setPaperId("p1");
        when(paperService.getPapersByHybridSearch(anyString(), anyInt(), anyInt(), any()))
                .thenReturn(List.of(paper));
        when(paperMapper.toDto(any())).thenReturn(null);

        String requestBody = """
                {
                  "query": "deep learning",
                  "pageSize": 10,
                  "page": 0,
                  "filters": [{"filter_name": "language", "filter_option": "en"}]
                }
                """;

        mockMvc.perform(post("/api/papers/hybrid-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // pageSize out of range (> 50) -> 400
    // -------------------------------------------------------------------------

    @Test
    void hybridSearch_pageSizeTooLarge_returns400() throws Exception {
        String requestBody = """
                {
                  "query": "deep learning",
                  "pageSize": 51,
                  "page": 0
                }
                """;

        mockMvc.perform(post("/api/papers/hybrid-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // pageSize below minimum (< 1) -> 400
    // -------------------------------------------------------------------------

    @Test
    void hybridSearch_pageSizeTooSmall_returns400() throws Exception {
        String requestBody = """
                {
                  "query": "deep learning",
                  "pageSize": 0,
                  "page": 0
                }
                """;

        mockMvc.perform(post("/api/papers/hybrid-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // page * pageSize >= 10000 -> 400
    // -------------------------------------------------------------------------

    @Test
    void hybridSearch_pageTooLarge_returns400() throws Exception {
        String requestBody = """
                {
                  "query": "deep learning",
                  "pageSize": 50,
                  "page": 200
                }
                """;

        mockMvc.perform(post("/api/papers/hybrid-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // Null query -> 400 (validation)
    // -------------------------------------------------------------------------

    @Test
    void hybridSearch_nullQuery_returns400() throws Exception {
        String requestBody = """
                {
                  "query": null,
                  "pageSize": 10,
                  "page": 0
                }
                """;

        mockMvc.perform(post("/api/papers/hybrid-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // No results from either leg -> 404 (service throws PaperNotInElasticException)
    // -------------------------------------------------------------------------

    @Test
    void hybridSearch_noResults_returns404() throws Exception {
        when(paperService.getPapersByHybridSearch(anyString(), anyInt(), anyInt(), isNull()))
                .thenThrow(new PaperNotInElasticException("There is no paper in the elastic for the page 0 and this query deep learning"));

        String requestBody = """
                {
                  "query": "deep learning",
                  "pageSize": 10,
                  "page": 0
                }
                """;

        mockMvc.perform(post("/api/papers/hybrid-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound());
    }
}
