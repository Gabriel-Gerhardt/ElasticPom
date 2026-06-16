package com.elasticpom.core.service;

import com.elasticpom.adapters.PaperMapper;
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

    // -------------------------------------------------------------------------
    // Null query → PaperNotInElasticException
    // -------------------------------------------------------------------------

    @Test
    void getPapersBySemanticSearch_nullQuery_throwsPaperNotInElasticException() {
        assertThatThrownBy(() ->
                paperService.getPapersBySemanticSearch(null, buildVector(), 10, 0))
                .isInstanceOf(PaperNotInElasticException.class)
                .hasMessageContaining("Query cannot be null");
    }

    // -------------------------------------------------------------------------
    // Valid vector + query → calls elasticsearchOperations with kNN query
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

        paperService.getPapersBySemanticSearch("deep learning", buildVector(), 10, 0);

        verify(elasticsearchOperations).search(any(Query.class), eq(ElasticPaperDocument.class));
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
                paperService.getPapersBySemanticSearch("neural networks", buildVector(), 10, 0))
                .isInstanceOf(PaperNotInElasticException.class)
                .hasMessageContaining("neural networks");
    }
}
