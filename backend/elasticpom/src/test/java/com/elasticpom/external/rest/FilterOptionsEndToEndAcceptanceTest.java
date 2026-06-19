package com.elasticpom.external.rest;

import com.elasticpom.adapters.PaperMapper;
import com.elasticpom.core.service.PaperService;
import com.elasticpom.core.service.embedding.EmbeddingService;
import com.elasticpom.external.document.ElasticPaperDocument;
import com.elasticpom.external.integration.ElasticPaperRepository;
import com.elasticpom.external.integration.PaperRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Acceptance test exercising the REAL PaperController -> PaperService wiring
 * for POST /api/papers/filter-options (PaperSearchRangeFilterAcceptanceTest
 * covers the same pattern for /search-by-query, but nothing previously wired
 * the real PaperService into the filter-options endpoint end to end).
 * <p>
 * Covers user-facing scenarios that the mocked-service tests in
 * PaperControllerFilterTest and the pure-unit tests in PaperServiceFilterTest
 * do not: a real HTTP request/response cycle that combines a free-text query
 * with an active filter on one field while requesting options for a
 * *different* field, proving the bool query that reaches Elasticsearch
 * actually carries both the multi-match clause and the other field's term
 * filter together (not just verified separately at the unit level). Also
 * covers malformed-body / missing-field controller-level validation and an
 * end-to-end 404 for a filter name that doesn't exist in the index mapping
 * (e.g. a stale frontend cache still asking for the removed "contributors"
 * filter).
 */
@WebMvcTest(PaperController.class)
@Import(PaperService.class)
class FilterOptionsEndToEndAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaperMapper paperMapper;

    @MockitoBean
    private PaperRepository paperRepository;

    @MockitoBean
    private ElasticPaperRepository elasticPaperRepository;

    @MockitoBean
    private ElasticsearchOperations elasticsearchOperations;

    @MockitoBean
    private EmbeddingService embeddingService;

    @SuppressWarnings("unchecked")
    @Test
    void getFilterOptions_queryPlusFilterOnFieldA_scopesAggregationForFieldB() throws Exception {
        // "language" (field A) is keyword, "subjects" (field B) is text+keyword.
        Map<String, Object> languageMapping = Map.of("type", "keyword");
        Map<String, Object> subjectsMapping = Map.of(
                "type", "text",
                "fields", Map.of("keyword", Map.of("type", "keyword")));
        Map<String, Object> properties = Map.of("language", languageMapping, "subjects", subjectsMapping);
        Map<String, Object> mapping = Map.of("properties", properties);

        IndexOperations indexOperations = mock(IndexOperations.class);
        when(elasticsearchOperations.indexOps(ElasticPaperDocument.class)).thenReturn(indexOperations);
        when(indexOperations.getMapping()).thenReturn(mapping);

        co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate sterms =
                co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate.of(s -> s
                        .buckets(b -> b.array(java.util.List.of(
                                co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket.of(
                                        t -> t.key(k -> k.stringValue("ai")).docCount(4))
                        ))));
        co.elastic.clients.elasticsearch._types.aggregations.Aggregate aggregate =
                co.elastic.clients.elasticsearch._types.aggregations.Aggregate.of(a -> a.sterms(sterms));
        org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations aggregationsContainer =
                new org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations(
                        Map.of("distinct_values", aggregate));

        @SuppressWarnings("rawtypes")
        SearchHits searchHits = mock(SearchHits.class);
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        when(elasticsearchOperations.search(queryCaptor.capture(), org.mockito.ArgumentMatchers.eq(ElasticPaperDocument.class)))
                .thenReturn(searchHits);
        when(searchHits.getAggregations()).thenReturn(aggregationsContainer);

        String requestBody = """
                {
                  "query": "deep learning",
                  "filter_name": "subjects",
                  "filters": [
                    {"filter_name": "language", "filter_option": "en"}
                  ]
                }
                """;

        mockMvc.perform(post("/api/papers/filter-options")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().json("[\"ai\"]"));

        NativeQuery nativeQuery = (NativeQuery) queryCaptor.getValue();
        co.elastic.clients.elasticsearch._types.query_dsl.Query boolQuery = nativeQuery.getQuery();

        // The free-text query is preserved as the must clause...
        assertThat(boolQuery.bool().must().get(0).multiMatch().query()).isEqualTo("deep learning");
        // ...and the OTHER field's (language) filter is preserved as a filter clause,
        // proving query+filterA are combined together when scoping field B's options,
        // not just exercised independently.
        assertThat(boolQuery.bool().filter()).hasSize(1);
        assertThat(boolQuery.bool().filter().get(0).term().field()).isEqualTo("language");
        assertThat(boolQuery.bool().filter().get(0).term().value().stringValue()).isEqualTo("en");
        // The aggregation itself targets the resolved (.keyword) field for "subjects".
        assertThat(nativeQuery.getAggregations().get("distinct_values").terms().field())
                .isEqualTo("subjects.keyword");
    }

    @Test
    void getFilterOptions_staleFilterNameNotInMapping_returns404NotUnhandledException() throws Exception {
        // Simulates a stale frontend cache still requesting options for the
        // removed "contributors" filter after the ES mapping/Mongo Filters
        // collection have moved on to "creators". Must fail gracefully (404
        // via InvalidFilterException -> GlobalExceptionHandler), not surface
        // a 500 or an unhandled exception.
        Map<String, Object> properties = Map.of("creators", Map.of(
                "type", "text", "fields", Map.of("keyword", Map.of("type", "keyword"))));
        Map<String, Object> mapping = Map.of("properties", properties);

        IndexOperations indexOperations = mock(IndexOperations.class);
        when(elasticsearchOperations.indexOps(ElasticPaperDocument.class)).thenReturn(indexOperations);
        when(indexOperations.getMapping()).thenReturn(mapping);

        String requestBody = """
                {
                  "filter_name": "contributors"
                }
                """;

        mockMvc.perform(post("/api/papers/filter-options")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void getFilterOptions_emptyBody_returns400() throws Exception {
        mockMvc.perform(post("/api/papers/filter-options")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getFilterOptions_missingFilterName_returns400() throws Exception {
        // filter_name is @NotNull on FilterOptionsRequest; omitting it entirely
        // must trigger bean validation (400), not reach the service layer at all.
        String requestBody = """
                {
                  "query": "deep learning"
                }
                """;

        mockMvc.perform(post("/api/papers/filter-options")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getFilterOptions_filterNameExplicitNull_returns400() throws Exception {
        String requestBody = """
                {
                  "query": "deep learning",
                  "filter_name": null
                }
                """;

        mockMvc.perform(post("/api/papers/filter-options")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getFilterOptions_malformedJsonBody_returns400() throws Exception {
        String malformedBody = "{ \"filter_name\": \"language\", "; // truncated/invalid JSON

        mockMvc.perform(post("/api/papers/filter-options")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedBody))
                .andExpect(status().isBadRequest());
    }

    @SuppressWarnings("unchecked")
    @Test
    void getFilterOptions_filtersExplicitlyNull_treatedSameAsOmitted() throws Exception {
        // "filters": null must behave identically to omitting the field entirely
        // (PaperService treats a null filters list as "no scoping filters").
        Map<String, Object> properties = Map.of("language", Map.of("type", "keyword"));
        Map<String, Object> mapping = Map.of("properties", properties);

        IndexOperations indexOperations = mock(IndexOperations.class);
        when(elasticsearchOperations.indexOps(ElasticPaperDocument.class)).thenReturn(indexOperations);
        when(indexOperations.getMapping()).thenReturn(mapping);

        co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate sterms =
                co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate.of(s -> s
                        .buckets(b -> b.array(java.util.List.of())));
        co.elastic.clients.elasticsearch._types.aggregations.Aggregate aggregate =
                co.elastic.clients.elasticsearch._types.aggregations.Aggregate.of(a -> a.sterms(sterms));
        org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations aggregationsContainer =
                new org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations(
                        Map.of("distinct_values", aggregate));

        @SuppressWarnings("rawtypes")
        SearchHits searchHits = mock(SearchHits.class);
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        when(elasticsearchOperations.search(queryCaptor.capture(), org.mockito.ArgumentMatchers.eq(ElasticPaperDocument.class)))
                .thenReturn(searchHits);
        when(searchHits.getAggregations()).thenReturn(aggregationsContainer);

        String requestBody = """
                {
                  "filter_name": "language",
                  "filters": null
                }
                """;

        mockMvc.perform(post("/api/papers/filter-options")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        NativeQuery nativeQuery = (NativeQuery) queryCaptor.getValue();
        co.elastic.clients.elasticsearch._types.query_dsl.Query boolQuery = nativeQuery.getQuery();
        assertThat(boolQuery.bool().filter()).isEmpty();
        // null/blank query becomes match_all.
        assertThat(boolQuery.bool().must().get(0).isMatchAll()).isTrue();
    }
}
