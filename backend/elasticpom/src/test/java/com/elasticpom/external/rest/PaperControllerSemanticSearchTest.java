package com.elasticpom.external.rest;

import com.elasticpom.adapters.PaperMapper;
import com.elasticpom.core.model.Paper;
import com.elasticpom.core.service.PaperService;
import com.elasticpom.exception.EmbeddingGenerationException;
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
class PaperControllerSemanticSearchTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaperService paperService;

    @MockitoBean
    private PaperMapper paperMapper;

    // -------------------------------------------------------------------------
    // Valid request (text query only - no queryVector) → 200
    // -------------------------------------------------------------------------

    @Test
    void semanticSearch_validRequest_returns200() throws Exception {
        Paper paper = new Paper();
        paper.setPaperId("p1");
        when(paperService.getPapersBySemanticSearch(anyString(), anyInt(), anyInt()))
                .thenReturn(List.of(paper));
        when(paperMapper.toDto(any())).thenReturn(null);

        String requestBody = """
                {
                  "query": "deep learning",
                  "pageSize": 10,
                  "page": 0
                }
                """;

        mockMvc.perform(post("/api/papers/semantic-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // page * pageSize >= 10000 → 400
    // -------------------------------------------------------------------------

    @Test
    void semanticSearch_pageTooLarge_returns400() throws Exception {
        String requestBody = """
                {
                  "query": "deep learning",
                  "pageSize": 50,
                  "page": 200
                }
                """;

        mockMvc.perform(post("/api/papers/semantic-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // Null query → 404 (service throws PaperNotInElasticException)
    // -------------------------------------------------------------------------

    @Test
    void semanticSearch_nullQuery_returns404() throws Exception {
        when(paperService.getPapersBySemanticSearch(isNull(), anyInt(), anyInt()))
                .thenThrow(new PaperNotInElasticException("Query cannot be null"));

        String requestBody = """
                {
                  "query": null,
                  "pageSize": 10,
                  "page": 0
                }
                """;

        mockMvc.perform(post("/api/papers/semantic-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // Embedding generation fails → 500, via GlobalExceptionHandler's
    // EmbeddingGenerationException mapping (semantic search has no BM25 fallback
    // leg, so the exception must propagate to the client as a 500, not a silent
    // empty result or a 404).
    // -------------------------------------------------------------------------

    @Test
    void semanticSearch_embeddingGenerationFails_returns500() throws Exception {
        when(paperService.getPapersBySemanticSearch(anyString(), anyInt(), anyInt()))
                .thenThrow(new EmbeddingGenerationException("Failed to generate embedding for query text", new RuntimeException("boom")));

        String requestBody = """
                {
                  "query": "deep learning",
                  "pageSize": 10,
                  "page": 0
                }
                """;

        mockMvc.perform(post("/api/papers/semantic-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isInternalServerError());
    }

    // -------------------------------------------------------------------------
    // pageSize out of range (> 50) → 400
    // -------------------------------------------------------------------------

    @Test
    void semanticSearch_pageSizeTooLarge_returns400() throws Exception {
        String requestBody = """
                {
                  "query": "deep learning",
                  "pageSize": 51,
                  "page": 0
                }
                """;

        mockMvc.perform(post("/api/papers/semantic-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // pageSize below minimum (< 1) → 400
    // -------------------------------------------------------------------------

    @Test
    void semanticSearch_pageSizeTooSmall_returns400() throws Exception {
        String requestBody = """
                {
                  "query": "deep learning",
                  "pageSize": 0,
                  "page": 0
                }
                """;

        mockMvc.perform(post("/api/papers/semantic-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }
}
