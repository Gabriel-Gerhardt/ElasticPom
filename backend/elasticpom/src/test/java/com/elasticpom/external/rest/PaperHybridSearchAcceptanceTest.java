package com.elasticpom.external.rest;

import com.elasticpom.adapters.PaperMapperImpl;
import com.elasticpom.core.service.PaperService;
import com.elasticpom.external.document.ElasticPaperDocument;
import com.elasticpom.external.document.PaperDocument;
import com.elasticpom.external.integration.ElasticPaperRepository;
import com.elasticpom.external.integration.PaperRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end acceptance test for POST /api/papers/hybrid-search, exercising the REAL
 * PaperController -> PaperService -> RrfMerger -> PaperMapperImpl wiring (via Spring's
 * @WebMvcTest + @Import of the concrete service and generated mapper), with only the
 * lowest-level boundaries (Mongo PaperRepository, ES repository/operations) mocked.
 * <p>
 * This complements PaperControllerHybridSearchTest (which mocks PaperService entirely,
 * so only proves HTTP-layer validation/routing) and PaperServiceHybridSearchTest (which
 * unit-tests the service/RRF call with mocked ES boundaries, but never goes through the
 * controller or the real mapper). Here both real classes - including the real RrfMerger
 * fusion logic - are wired together end to end and exercised through MockMvc, the same
 * pattern PaperSearchRangeFilterAcceptanceTest uses for the syntactic search-by-query
 * endpoint.
 * <p>
 * No live Elasticsearch testcontainer is available in this sandbox (none of the existing
 * acceptance tests in this repo use one either - they all mock at the
 * ElasticPaperRepository/ElasticsearchOperations boundary), so this test follows the same
 * convention rather than introducing a new, heavier test dependency.
 */
@WebMvcTest(PaperController.class)
@Import({PaperService.class, PaperMapperImpl.class})
class PaperHybridSearchAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaperRepository paperRepository;

    @MockitoBean
    private ElasticPaperRepository elasticPaperRepository;

    @MockitoBean
    private ElasticsearchOperations elasticsearchOperations;

    private static String buildVectorJson() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 384; i++) {
            if (i > 0) sb.append(",");
            sb.append("0.1");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String requestBody(int pageSize, int page) {
        return """
                {
                  "query": "deep learning",
                  "queryVector": %s,
                  "pageSize": %d,
                  "page": %d
                }
                """.formatted(buildVectorJson(), pageSize, page);
    }

    private void mockSyntactic(String... ids) {
        List<ElasticPaperDocument> docs = Stream.of(ids)
                .map(id -> ElasticPaperDocument.builder().id(id).build())
                .toList();
        Page<ElasticPaperDocument> page = new PageImpl<>(docs);
        when(elasticPaperRepository.findByQuery(any(), any())).thenReturn(page);
    }

    @SuppressWarnings("unchecked")
    private void mockSemantic(String... ids) {
        List<SearchHit<ElasticPaperDocument>> hits = Stream.of(ids)
                .map(id -> {
                    SearchHit<ElasticPaperDocument> hit = mock(SearchHit.class);
                    when(hit.getContent()).thenReturn(ElasticPaperDocument.builder().id(id).build());
                    return hit;
                })
                .toList();
        SearchHits<ElasticPaperDocument> searchHits = mock(SearchHits.class);
        when(elasticsearchOperations.search(any(Query.class), eq(ElasticPaperDocument.class)))
                .thenReturn(searchHits);
        when(searchHits.stream()).thenReturn(hits.stream());
    }

    private void mockPaperDocument(String paperId, String title) {
        PaperDocument doc = new PaperDocument();
        doc.setPaperId(paperId);
        doc.setTitle(title);
        when(paperRepository.findByPaperId(paperId)).thenReturn(doc);
    }

    // -------------------------------------------------------------------------
    // Real flow: distinct ids in each leg -> real RRF merge -> real mapper -> JSON
    // -------------------------------------------------------------------------

    @Test
    void hybridSearch_realFlow_mergesBothLegsAndMapsRealPaperFields() throws Exception {
        mockSyntactic("paper-a", "paper-b", "paper-c");
        mockSemantic("paper-d", "paper-e");
        mockPaperDocument("paper-a", "Title A");
        mockPaperDocument("paper-b", "Title B");
        mockPaperDocument("paper-c", "Title C");
        mockPaperDocument("paper-d", "Title D");
        mockPaperDocument("paper-e", "Title E");

        mockMvc.perform(post("/api/papers/hybrid-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(10, 0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                // Top syntactic hit, rank 1, contributes the highest RRF score since it
                // doesn't also appear in the semantic leg's first rank.
                .andExpect(jsonPath("$[0].paperId").value("paper-a"))
                .andExpect(jsonPath("$[0].title").value("Title A"));
    }

    // -------------------------------------------------------------------------
    // Duplicate id across both legs: must be deduped, not duplicated, in the
    // real merged output (RrfMerger + real getPapersByIds lookup chain).
    // -------------------------------------------------------------------------

    @Test
    void hybridSearch_idInBothLegs_isDedupedInRealMergedOutput() throws Exception {
        mockSyntactic("shared", "only-syntactic");
        mockSemantic("shared", "only-semantic");
        mockPaperDocument("shared", "Shared Paper");
        mockPaperDocument("only-syntactic", "Syntactic Only");
        mockPaperDocument("only-semantic", "Semantic Only");

        mockMvc.perform(post("/api/papers/hybrid-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(10, 0)))
                .andExpect(status().isOk())
                // 3 distinct ids total, not 4 - "shared" must not be duplicated.
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].paperId").value("shared"));
    }

    // -------------------------------------------------------------------------
    // Very small pageSize (1): real merge must cap output to exactly 1, picking
    // the highest-RRF-score id across both legs.
    // -------------------------------------------------------------------------

    @Test
    void hybridSearch_pageSizeOfOne_capsRealMergedOutputToSingleTopResult() throws Exception {
        mockSyntactic("top", "second");
        mockSemantic("top", "third");
        mockPaperDocument("top", "Top Paper");
        mockPaperDocument("second", "Second Paper");

        mockMvc.perform(post("/api/papers/hybrid-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(1, 0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].paperId").value("top"));
    }

    // -------------------------------------------------------------------------
    // Syntactic leg empty -> real flow falls back to semantic leg unchanged,
    // through the real service (not just asserted at the unit level).
    // -------------------------------------------------------------------------

    @Test
    void hybridSearch_realFlow_syntacticLegEmpty_fallsBackToSemanticLegUnchanged() throws Exception {
        mockSyntactic();
        mockSemantic("sem-1", "sem-2");
        mockPaperDocument("sem-1", "Semantic One");
        mockPaperDocument("sem-2", "Semantic Two");

        mockMvc.perform(post("/api/papers/hybrid-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(10, 0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].paperId").value("sem-1"))
                .andExpect(jsonPath("$[1].paperId").value("sem-2"));
    }

    // -------------------------------------------------------------------------
    // Both legs empty -> real service throws PaperNotInElasticException -> 404
    // via the real GlobalExceptionHandler-mapped flow.
    // -------------------------------------------------------------------------

    @Test
    void hybridSearch_realFlow_bothLegsEmpty_returns404() throws Exception {
        mockSyntactic();
        mockSemantic();

        mockMvc.perform(post("/api/papers/hybrid-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(10, 0)))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // A paper id returned by ES but missing from the Mongo lookup (paperRepository
    // returns null) must be silently filtered out of the real merged + mapped
    // response rather than causing an NPE - matching getPapersByIds' existing
    // Objects::nonNull filter, now exercised through the hybrid path end to end.
    // -------------------------------------------------------------------------

    @Test
    void hybridSearch_realFlow_idMissingFromMongo_isFilteredOutOfMergedResponse() throws Exception {
        mockSyntactic("present", "missing-from-mongo");
        mockSemantic();
        mockPaperDocument("present", "Present Paper");
        when(paperRepository.findByPaperId("missing-from-mongo")).thenReturn(null);

        mockMvc.perform(post("/api/papers/hybrid-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(10, 0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].paperId").value("present"));
    }
}
