package com.elasticpom.core.service;

import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
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

    // -------------------------------------------------------------------------
    // Bug 1: Range filter with explicit from/to values produces correct gte/lte
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void getPapersByQuery_rangeFilterWithEndValue_setsDistinctGteAndLte() {
        Map<String, Object> properties = Map.of("datestamp", Map.of("type", "date"));
        Map<String, Object> mapping = Map.of("properties", properties);

        when(elasticsearchOperations.indexOps(ElasticPaperDocument.class)).thenReturn(indexOperations);
        when(indexOperations.getMapping()).thenReturn(mapping);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        when(elasticsearchOperations.search(queryCaptor.capture(), eq(ElasticPaperDocument.class)))
                .thenReturn(searchHits);
        when(searchHits.stream()).thenReturn(java.util.stream.Stream.of());

        FilterRequest rangeFilter = new FilterRequest("datestamp", "2020-01-01", "2023-12-31");

        assertThatThrownBy(() ->
                paperService.getPapersByQuery("query", 10, 0, List.of(rangeFilter)))
                .isInstanceOf(com.elasticpom.exception.PaperNotInElasticException.class);

        org.springframework.data.elasticsearch.client.elc.NativeQuery nativeQuery =
                (org.springframework.data.elasticsearch.client.elc.NativeQuery) queryCaptor.getValue();
        co.elastic.clients.elasticsearch._types.query_dsl.Query boolQuery = nativeQuery.getQuery();
        RangeQuery rangeQuery = boolQuery.bool().filter().get(0).range();

        assertThat(rangeQuery.untyped().gte().toString()).contains("2020-01-01");
        assertThat(rangeQuery.untyped().lte().toString()).contains("2023-12-31");
        assertThat(rangeQuery.untyped().gte()).isNotEqualTo(rangeQuery.untyped().lte());
    }

    // -------------------------------------------------------------------------
    // Bug 1: Range filter without an end value remains backward compatible
    // (gte == lte == filterOption, single-value match)
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void getPapersByQuery_rangeFilterWithoutEndValue_setsEqualGteAndLte() {
        Map<String, Object> properties = Map.of("datestamp", Map.of("type", "date"));
        Map<String, Object> mapping = Map.of("properties", properties);

        when(elasticsearchOperations.indexOps(ElasticPaperDocument.class)).thenReturn(indexOperations);
        when(indexOperations.getMapping()).thenReturn(mapping);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        when(elasticsearchOperations.search(queryCaptor.capture(), eq(ElasticPaperDocument.class)))
                .thenReturn(searchHits);
        when(searchHits.stream()).thenReturn(java.util.stream.Stream.of());

        FilterRequest rangeFilter = new FilterRequest("datestamp", "2023-06-15");

        assertThatThrownBy(() ->
                paperService.getPapersByQuery("query", 10, 0, List.of(rangeFilter)))
                .isInstanceOf(com.elasticpom.exception.PaperNotInElasticException.class);

        org.springframework.data.elasticsearch.client.elc.NativeQuery nativeQuery =
                (org.springframework.data.elasticsearch.client.elc.NativeQuery) queryCaptor.getValue();
        co.elastic.clients.elasticsearch._types.query_dsl.Query boolQuery = nativeQuery.getQuery();
        RangeQuery rangeQuery = boolQuery.bool().filter().get(0).range();

        assertThat(rangeQuery.untyped().gte().toString()).contains("2023-06-15");
        assertThat(rangeQuery.untyped().lte().toString()).contains("2023-06-15");
    }

    // -------------------------------------------------------------------------
    // Bug 1: Malformed end value for a range filter → BadRequestException
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void getPapersByQuery_malformedRangeEndValue_throwsBadRequestException() {
        Map<String, Object> properties = Map.of("year", Map.of("type", "integer"));
        Map<String, Object> mapping = Map.of("properties", properties);

        when(elasticsearchOperations.indexOps(ElasticPaperDocument.class)).thenReturn(indexOperations);
        when(indexOperations.getMapping()).thenReturn(mapping);

        FilterRequest badRange = new FilterRequest("year", "2020", "not-a-number");

        assertThatThrownBy(() ->
                paperService.getPapersByQuery("query", 10, 0, List.of(badRange)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not-a-number")
                .hasMessageContaining("year");
    }

    // -------------------------------------------------------------------------
    // Multi-field mapping: "base.sub" (e.g. "subjects.keyword") validates against
    // properties.base.fields.sub, while a truly unknown field is still rejected
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void getDistinctFilterValues_multiFieldName_validatesAgainstNestedFieldsMap() {
        Map<String, Object> keywordSubField = Map.of("type", "keyword");
        Map<String, Object> subjectsMapping = Map.of(
                "type", "text",
                "fields", Map.of("keyword", keywordSubField));
        Map<String, Object> properties = Map.of("subjects", subjectsMapping);
        Map<String, Object> mapping = Map.of("properties", properties);

        when(elasticsearchOperations.indexOps(ElasticPaperDocument.class)).thenReturn(indexOperations);
        when(indexOperations.getMapping()).thenReturn(mapping);

        co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate sterms =
                co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate.of(s -> s
                        .buckets(b -> b.array(List.of(
                                co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket.of(t -> t.key(k -> k.stringValue("ai")).docCount(3))
                        ))));
        co.elastic.clients.elasticsearch._types.aggregations.Aggregate aggregate =
                co.elastic.clients.elasticsearch._types.aggregations.Aggregate.of(a -> a.sterms(sterms));

        org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations aggregationsContainer =
                new org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations(
                        Map.of("distinct_values", aggregate));

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        when(elasticsearchOperations.search(queryCaptor.capture(), eq(ElasticPaperDocument.class)))
                .thenReturn(searchHits);
        when(searchHits.getAggregations()).thenReturn(aggregationsContainer);

        List<String> result = paperService.getDistinctFilterValues("subjects.keyword");

        assertThat(result).containsExactly("ai");

        org.springframework.data.elasticsearch.client.elc.NativeQuery nativeQuery =
                (org.springframework.data.elasticsearch.client.elc.NativeQuery) queryCaptor.getValue();
        co.elastic.clients.elasticsearch._types.aggregations.Aggregation termsAgg =
                nativeQuery.getAggregations().get("distinct_values");
        assertThat(termsAgg.terms().field()).isEqualTo("subjects.keyword");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getDistinctFilterValues_unknownSubField_throwsInvalidFilterException() {
        Map<String, Object> subjectsMapping = Map.of(
                "type", "text",
                "fields", Map.of("keyword", Map.of("type", "keyword")));
        Map<String, Object> properties = Map.of("subjects", subjectsMapping);
        Map<String, Object> mapping = Map.of("properties", properties);

        when(elasticsearchOperations.indexOps(ElasticPaperDocument.class)).thenReturn(indexOperations);
        when(indexOperations.getMapping()).thenReturn(mapping);

        assertThatThrownBy(() -> paperService.getDistinctFilterValues("subjects.nonexistent"))
                .isInstanceOf(InvalidFilterException.class)
                .hasMessageContaining("subjects.nonexistent");
    }

    // -------------------------------------------------------------------------
    // Bug 2: getDistinctFilterValues — invalid filter name throws InvalidFilterException
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void getDistinctFilterValues_invalidFilterName_throwsInvalidFilterException() {
        Map<String, Object> properties = Map.of("title", Map.of("type", "text"));
        Map<String, Object> mapping = Map.of("properties", properties);

        when(elasticsearchOperations.indexOps(ElasticPaperDocument.class)).thenReturn(indexOperations);
        when(indexOperations.getMapping()).thenReturn(mapping);

        assertThatThrownBy(() -> paperService.getDistinctFilterValues("nonexistent_field"))
                .isInstanceOf(InvalidFilterException.class)
                .hasMessageContaining("nonexistent_field");
    }

    // -------------------------------------------------------------------------
    // Bug 2: getDistinctFilterValues — returns distinct string bucket values
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void getDistinctFilterValues_validFilterName_returnsBucketKeys() {
        Map<String, Object> properties = Map.of("language", Map.of("type", "keyword"));
        Map<String, Object> mapping = Map.of("properties", properties);

        when(elasticsearchOperations.indexOps(ElasticPaperDocument.class)).thenReturn(indexOperations);
        when(indexOperations.getMapping()).thenReturn(mapping);

        co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate sterms =
                co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate.of(s -> s
                        .buckets(b -> b.array(List.of(
                                co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket.of(t -> t.key(k -> k.stringValue("en")).docCount(5)),
                                co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket.of(t -> t.key(k -> k.stringValue("fr")).docCount(2))
                        ))));
        co.elastic.clients.elasticsearch._types.aggregations.Aggregate aggregate =
                co.elastic.clients.elasticsearch._types.aggregations.Aggregate.of(a -> a.sterms(sterms));

        org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations aggregationsContainer =
                new org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations(
                        Map.of("distinct_values", aggregate));

        when(elasticsearchOperations.search(any(Query.class), eq(ElasticPaperDocument.class)))
                .thenReturn(searchHits);
        when(searchHits.getAggregations()).thenReturn(aggregationsContainer);

        List<String> result = paperService.getDistinctFilterValues("language");

        assertThat(result).containsExactlyInAnyOrder("en", "fr");
    }

    // -------------------------------------------------------------------------
    // Bug 2: getDistinctFilterValues — null aggregations container returns empty list
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void getDistinctFilterValues_noAggregations_returnsEmptyList() {
        Map<String, Object> properties = Map.of("language", Map.of("type", "keyword"));
        Map<String, Object> mapping = Map.of("properties", properties);

        when(elasticsearchOperations.indexOps(ElasticPaperDocument.class)).thenReturn(indexOperations);
        when(indexOperations.getMapping()).thenReturn(mapping);

        when(elasticsearchOperations.search(any(Query.class), eq(ElasticPaperDocument.class)))
                .thenReturn(searchHits);
        when(searchHits.getAggregations()).thenReturn(null);

        List<String> result = paperService.getDistinctFilterValues("language");

        assertThat(result).isEmpty();
    }
}
