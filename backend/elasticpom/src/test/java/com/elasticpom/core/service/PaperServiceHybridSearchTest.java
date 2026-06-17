package com.elasticpom.core.service;

import com.elasticpom.adapters.PaperMapper;
import com.elasticpom.core.model.Paper;
import com.elasticpom.exception.PaperNotInElasticException;
import com.elasticpom.external.document.ElasticPaperDocument;
import com.elasticpom.external.document.PaperDocument;
import com.elasticpom.external.integration.ElasticPaperRepository;
import com.elasticpom.external.integration.PaperRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaperServiceHybridSearchTest {

    @Mock
    private PaperRepository paperRepository;

    @Mock
    private ElasticPaperRepository elasticRepository;

    @Mock
    private PaperMapper paperMapper;

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    @SuppressWarnings("rawtypes")
    private SearchHits semanticSearchHits;

    private PaperService paperService;

    private static float[] buildVector() {
        float[] v = new float[384];
        for (int i = 0; i < 384; i++) {
            v[i] = 0.1f;
        }
        return v;
    }

    @BeforeEach
    void setUp() {
        paperService = new PaperService(paperRepository, elasticRepository, paperMapper, elasticsearchOperations);
    }

    private void mockSyntactic(String... ids) {
        List<ElasticPaperDocument> docs = Stream.of(ids)
                .map(id -> ElasticPaperDocument.builder().id(id).build())
                .toList();
        Page<ElasticPaperDocument> page = new PageImpl<>(docs);
        when(elasticRepository.findByQuery(any(), any())).thenReturn(page);
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
        when(elasticsearchOperations.search(any(Query.class), eq(ElasticPaperDocument.class)))
                .thenReturn(semanticSearchHits);
        when(semanticSearchHits.stream()).thenReturn(hits.stream());
    }

    private void mockPaperLookup() {
        when(paperRepository.findByPaperId(any())).thenReturn(new PaperDocument());
        when(paperMapper.fromDocument(any())).thenReturn(new Paper());
    }

    // -------------------------------------------------------------------------
    // Both legs return results -> merged via RRF, capped at pageSize
    // -------------------------------------------------------------------------

    @Test
    void getPapersByHybridSearch_bothLegsReturnResults_mergesAndCapsAtPageSize() {
        mockSyntactic("a", "b", "c");
        mockSemantic("b", "d", "e");
        mockPaperLookup();

        List<?> result = paperService.getPapersByHybridSearch("deep learning", buildVector(), 3, 0);

        assertThat(result).hasSize(3);
    }

    // -------------------------------------------------------------------------
    // Syntactic empty -> fall back to semantic results unchanged
    // -------------------------------------------------------------------------

    @Test
    void getPapersByHybridSearch_syntacticEmpty_fallsBackToSemanticResults() {
        mockSyntactic();
        mockSemantic("d", "e");
        mockPaperLookup();

        List<?> result = paperService.getPapersByHybridSearch("deep learning", buildVector(), 10, 0);

        assertThat(result).hasSize(2);
    }

    // -------------------------------------------------------------------------
    // Semantic empty -> fall back to syntactic results unchanged
    // -------------------------------------------------------------------------

    @Test
    void getPapersByHybridSearch_semanticEmpty_fallsBackToSyntacticResults() {
        mockSyntactic("a", "b");
        mockSemantic();
        mockPaperLookup();

        List<?> result = paperService.getPapersByHybridSearch("deep learning", buildVector(), 10, 0);

        assertThat(result).hasSize(2);
    }

    // -------------------------------------------------------------------------
    // Both empty -> PaperNotInElasticException, consistent with existing endpoints
    // -------------------------------------------------------------------------

    @Test
    void getPapersByHybridSearch_bothEmpty_throwsPaperNotInElasticException() {
        mockSyntactic();
        mockSemantic();

        assertThatThrownBy(() ->
                paperService.getPapersByHybridSearch("deep learning", buildVector(), 10, 0))
                .isInstanceOf(PaperNotInElasticException.class);
    }
}
