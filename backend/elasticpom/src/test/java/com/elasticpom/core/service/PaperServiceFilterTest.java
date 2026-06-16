package com.elasticpom.core.service;

import com.elasticpom.adapters.PaperMapper;
import com.elasticpom.adapters.dto.request.FilterRequest;
import com.elasticpom.exception.BadRequestException;
import com.elasticpom.exception.InvalidFilterException;
import com.elasticpom.external.document.ElasticPaperDocument;
import com.elasticpom.external.integration.ElasticPaperRepository;
import com.elasticpom.external.integration.PaperRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaperServiceFilterTest {

    @Mock
    private PaperRepository paperRepository;

    @Mock
    private ElasticPaperRepository elasticRepository;

    @Mock
    private PaperMapper paperMapper;

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private IndexOperations indexOperations;

    @Mock
    @SuppressWarnings("rawtypes")
    private SearchHits searchHits;

    private PaperService paperService;

    @BeforeEach
    void setUp() {
        paperService = new PaperService(paperRepository, elasticRepository, paperMapper, elasticsearchOperations);
    }

    // -------------------------------------------------------------------------
    // Null / empty filters fall back to elasticRepository.findByQuery()
    // -------------------------------------------------------------------------

    @Test
    void getPapersByQuery_nullFilters_delegatesToRepository() {
        ElasticPaperDocument doc = ElasticPaperDocument.builder().id("paper-1").build();
        Page<ElasticPaperDocument> page = new PageImpl<>(List.of(doc));
        when(elasticRepository.findByQuery(eq("machine learning"), any(Pageable.class))).thenReturn(page);
        when(paperRepository.findByPaperId("paper-1")).thenReturn(null);

        // Should not throw even when mongo returns null (filtered out)
        paperService.getPapersByQuery("machine learning", 10, 0, null);

        verify(elasticRepository).findByQuery(eq("machine learning"), any(Pageable.class));
        verifyNoInteractions(elasticsearchOperations);
    }

    @Test
    void getPapersByQuery_emptyFilters_delegatesToRepository() {
        ElasticPaperDocument doc = ElasticPaperDocument.builder().id("paper-2").build();
        Page<ElasticPaperDocument> page = new PageImpl<>(List.of(doc));
        when(elasticRepository.findByQuery(eq("neural networks"), any(Pageable.class))).thenReturn(page);
        when(paperRepository.findByPaperId("paper-2")).thenReturn(null);

        paperService.getPapersByQuery("neural networks", 10, 0, Collections.emptyList());

        verify(elasticRepository).findByQuery(eq("neural networks"), any(Pageable.class));
        verifyNoInteractions(elasticsearchOperations);
    }

    // -------------------------------------------------------------------------
    // Invalid filter name → InvalidFilterException
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void getPapersByQuery_invalidFilterName_throwsInvalidFilterException() {
        Map<String, Object> properties = Map.of("title", Map.of("type", "text"));
        Map<String, Object> mapping = Map.of("properties", properties);

        when(elasticsearchOperations.indexOps(ElasticPaperDocument.class)).thenReturn(indexOperations);
        when(indexOperations.getMapping()).thenReturn(mapping);

        FilterRequest badFilter = new FilterRequest("nonexistent_field", "someValue");

        assertThatThrownBy(() ->
                paperService.getPapersByQuery("query", 10, 0, List.of(badFilter)))
                .isInstanceOf(InvalidFilterException.class)
                .hasMessageContaining("nonexistent_field");
    }

    // -------------------------------------------------------------------------
    // Valid text/keyword filter → term query path (no range validation)
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void getPapersByQuery_validTextFilter_executesNativeQuery() {
        Map<String, Object> properties = Map.of("language", Map.of("type", "keyword"));
        Map<String, Object> mapping = Map.of("properties", properties);

        when(elasticsearchOperations.indexOps(ElasticPaperDocument.class)).thenReturn(indexOperations);
        when(indexOperations.getMapping()).thenReturn(mapping);

        ElasticPaperDocument doc = ElasticPaperDocument.builder().id("paper-10").build();
        SearchHit<ElasticPaperDocument> hit = mock(SearchHit.class);
        when(hit.getContent()).thenReturn(doc);

        when(elasticsearchOperations.search(any(Query.class), eq(ElasticPaperDocument.class)))
                .thenReturn(searchHits);
        when(searchHits.stream()).thenReturn(java.util.stream.Stream.of(hit));

        when(paperRepository.findByPaperId("paper-10")).thenReturn(null);

        FilterRequest textFilter = new FilterRequest("language", "en");
        paperService.getPapersByQuery("deep learning", 10, 0, List.of(textFilter));

        verify(elasticsearchOperations).search(any(Query.class), eq(ElasticPaperDocument.class));
        verifyNoInteractions(elasticRepository);
    }

    // -------------------------------------------------------------------------
    // Valid numeric filter → range query path (numeric validation passes)
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void getPapersByQuery_validIntegerFilter_executesNativeQuery() {
        Map<String, Object> properties = Map.of("year", Map.of("type", "integer"));
        Map<String, Object> mapping = Map.of("properties", properties);

        when(elasticsearchOperations.indexOps(ElasticPaperDocument.class)).thenReturn(indexOperations);
        when(indexOperations.getMapping()).thenReturn(mapping);

        when(elasticsearchOperations.search(any(Query.class), eq(ElasticPaperDocument.class)))
                .thenReturn(searchHits);
        when(searchHits.stream()).thenReturn(java.util.stream.Stream.of());

        FilterRequest numericFilter = new FilterRequest("year", "2023");

        // Empty results → PaperNotInElasticException, but the range path was taken
        assertThatThrownBy(() ->
                paperService.getPapersByQuery("query", 10, 0, List.of(numericFilter)))
                .isInstanceOf(com.elasticpom.exception.PaperNotInElasticException.class);

        verify(elasticsearchOperations).search(any(Query.class), eq(ElasticPaperDocument.class));
        verifyNoInteractions(elasticRepository);
    }

    // -------------------------------------------------------------------------
    // Malformed numeric value for integer field → BadRequestException
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void getPapersByQuery_malformedNumericValue_throwsBadRequestException() {
        Map<String, Object> properties = Map.of("year", Map.of("type", "integer"));
        Map<String, Object> mapping = Map.of("properties", properties);

        when(elasticsearchOperations.indexOps(ElasticPaperDocument.class)).thenReturn(indexOperations);
        when(indexOperations.getMapping()).thenReturn(mapping);

        FilterRequest badNumeric = new FilterRequest("year", "not-a-number");

        assertThatThrownBy(() ->
                paperService.getPapersByQuery("query", 10, 0, List.of(badNumeric)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not-a-number")
                .hasMessageContaining("year");
    }

    // -------------------------------------------------------------------------
    // Malformed date value for date field → BadRequestException
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void getPapersByQuery_malformedDateValue_throwsBadRequestException() {
        Map<String, Object> properties = Map.of("datestamp", Map.of("type", "date"));
        Map<String, Object> mapping = Map.of("properties", properties);

        when(elasticsearchOperations.indexOps(ElasticPaperDocument.class)).thenReturn(indexOperations);
        when(indexOperations.getMapping()).thenReturn(mapping);

        FilterRequest badDate = new FilterRequest("datestamp", "31-13-9999");

        assertThatThrownBy(() ->
                paperService.getPapersByQuery("query", 10, 0, List.of(badDate)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("31-13-9999")
                .hasMessageContaining("datestamp");
    }

    // -------------------------------------------------------------------------
    // Valid date value for date field → range query path (no exception)
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void getPapersByQuery_validDateFilter_executesNativeQuery() {
        Map<String, Object> properties = Map.of("datestamp", Map.of("type", "date"));
        Map<String, Object> mapping = Map.of("properties", properties);

        when(elasticsearchOperations.indexOps(ElasticPaperDocument.class)).thenReturn(indexOperations);
        when(indexOperations.getMapping()).thenReturn(mapping);

        when(elasticsearchOperations.search(any(Query.class), eq(ElasticPaperDocument.class)))
                .thenReturn(searchHits);
        when(searchHits.stream()).thenReturn(java.util.stream.Stream.of());

        FilterRequest dateFilter = new FilterRequest("datestamp", "2023-06-15");

        assertThatThrownBy(() ->
                paperService.getPapersByQuery("query", 10, 0, List.of(dateFilter)))
                .isInstanceOf(com.elasticpom.exception.PaperNotInElasticException.class);

        verify(elasticsearchOperations).search(any(Query.class), eq(ElasticPaperDocument.class));
    }

    // -------------------------------------------------------------------------
    // Null mapping properties → InvalidFilterException
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void getPapersByQuery_nullMappingProperties_throwsInvalidFilterException() {
        // Mapping has no "properties" key → properties will be null
        Map<String, Object> mapping = Map.of();

        when(elasticsearchOperations.indexOps(ElasticPaperDocument.class)).thenReturn(indexOperations);
        when(indexOperations.getMapping()).thenReturn(mapping);

        FilterRequest filter = new FilterRequest("title", "some value");

        assertThatThrownBy(() ->
                paperService.getPapersByQuery("query", 10, 0, List.of(filter)))
                .isInstanceOf(InvalidFilterException.class)
                .hasMessageContaining("unavailable");
    }
}
