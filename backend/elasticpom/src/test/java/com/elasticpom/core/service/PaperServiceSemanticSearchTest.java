package com.elasticpom.core.service;

import com.elasticpom.adapters.PaperMapper;
import com.elasticpom.core.service.embedding.EmbeddingService;
import com.elasticpom.exception.PaperNotInElasticException;
import com.elasticpom.external.document.ElasticPaperDocument;
import com.elasticpom.external.integration.ElasticPaperRepository;
import com.elasticpom.external.integration.PaperRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaperServiceSemanticSearchTest {

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
    private SearchHits searchHits;

    @Mock
    private EmbeddingService embeddingService;

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
        paperService = new PaperService(paperRepository, elasticRepository, paperMapper, elasticsearchOperations, embeddingService);
        lenient().when(embeddingService.embed(any())).thenReturn(buildVector());
    }

    // -------------------------------------------------------------------------
    // Null query → PaperNotInElasticException
    // -------------------------------------------------------------------------

    @Test
    void getPapersBySemanticSearch_nullQuery_throwsPaperNotInElasticException() {
        assertThatThrownBy(() ->
                paperService.getPapersBySemanticSearch(null, 10, 0))
                .isInstanceOf(PaperNotInElasticException.class)
                .hasMessageContaining("Query cannot be null");
    }

    // -------------------------------------------------------------------------
    // Valid query → embeds server-side, then calls elasticsearchOperations with kNN query
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void getPapersBySemanticSearch_validRequest_callsElasticsearchOperations() {
        ElasticPaperDocument doc = ElasticPaperDocument.builder().id("paper-1").build();
        SearchHit<ElasticPaperDocument> hit = mock(SearchHit.class);
        when(hit.getContent()).thenReturn(doc);

        when(elasticsearchOperations.search(any(Query.class), eq(ElasticPaperDocument.class)))
                .thenReturn(searchHits);
        when(searchHits.stream()).thenReturn(java.util.stream.Stream.of(hit));
        when(paperRepository.findByPaperId("paper-1")).thenReturn(null);

        paperService.getPapersBySemanticSearch("deep learning", 10, 0);

        verify(embeddingService).embed("deep learning");
        verify(elasticsearchOperations).search(any(Query.class), eq(ElasticPaperDocument.class));
    }

    // -------------------------------------------------------------------------
    // Blank (whitespace-only, non-null) query → NOT rejected by the null guard
    // (only `query == null` is checked, see getPapersBySemanticSearch), so it is
    // passed straight through to the embedding service as-is. This documents
    // current behavior: there is no blank-query validation here (unlike
    // buildFilteredBoolQuery's BM25 path, which treats blank as "match all" -
    // semantic search has no such fallback and will embed the literal blank
    // string instead).
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void getPapersBySemanticSearch_blankQuery_isNotRejectedAndReachesEmbeddingServiceAsIs() {
        ElasticPaperDocument doc = ElasticPaperDocument.builder().id("paper-1").build();
        SearchHit<ElasticPaperDocument> hit = mock(SearchHit.class);
        when(hit.getContent()).thenReturn(doc);

        when(elasticsearchOperations.search(any(Query.class), eq(ElasticPaperDocument.class)))
                .thenReturn(searchHits);
        when(searchHits.stream()).thenReturn(java.util.stream.Stream.of(hit));
        when(paperRepository.findByPaperId("paper-1")).thenReturn(null);

        paperService.getPapersBySemanticSearch("   ", 10, 0);

        verify(embeddingService).embed("   ");
    }

    // -------------------------------------------------------------------------
    // Empty results → PaperNotInElasticException
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void getPapersBySemanticSearch_emptyResults_throwsPaperNotInElasticException() {
        when(elasticsearchOperations.search(any(Query.class), eq(ElasticPaperDocument.class)))
                .thenReturn(searchHits);
        when(searchHits.stream()).thenReturn(java.util.stream.Stream.of());

        assertThatThrownBy(() ->
                paperService.getPapersBySemanticSearch("neural networks", 10, 0))
                .isInstanceOf(PaperNotInElasticException.class)
                .hasMessageContaining("neural networks");
    }
}
