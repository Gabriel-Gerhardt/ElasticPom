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

    private static String buildVector() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 384; i++) {
            if (i > 0) sb.append(",");
            sb.append("0.1");
        }
        sb.append("]");
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Valid request -> 200
    // -------------------------------------------------------------------------

    @Test
    void hybridSearch_validRequest_returns200() throws Exception {
        Paper paper = new Paper();
        paper.setPaperId("p1");
        when(paperService.getPapersByHybridSearch(anyString(), any(float[].class), anyInt(), anyInt()))
                .thenReturn(List.of(paper));
        when(paperMapper.toDto(any())).thenReturn(null);

        String requestBody = """
                {
                  "query": "deep learning",
                  "queryVector": %s,
                  "pageSize": 10,
                  "page": 0
                }
                """.formatted(buildVector());

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
                  "queryVector": %s,
                  "pageSize": 51,
                  "page": 0
                }
                """.formatted(buildVector());

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
                  "queryVector": %s,
                  "pageSize": 0,
                  "page": 0
                }
                """.formatted(buildVector());

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
                  "queryVector": %s,
                  "pageSize": 50,
                  "page": 200
                }
                """.formatted(buildVector());

        mockMvc.perform(post("/api/papers/hybrid-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // Null queryVector -> 400 (validation)
    // -------------------------------------------------------------------------

    @Test
    void hybridSearch_nullQueryVector_returns400() throws Exception {
        String requestBody = """
                {
                  "query": "deep learning",
                  "queryVector": null,
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
        when(paperService.getPapersByHybridSearch(anyString(), any(float[].class), anyInt(), anyInt()))
                .thenThrow(new PaperNotInElasticException("There is no paper in the elastic for the page 0 and this query deep learning"));

        String requestBody = """
                {
                  "query": "deep learning",
                  "queryVector": %s,
                  "pageSize": 10,
                  "page": 0
                }
                """.formatted(buildVector());

        mockMvc.perform(post("/api/papers/hybrid-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound());
    }
}
