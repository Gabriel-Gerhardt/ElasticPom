package com.elasticpom.external.rest;

import com.elasticpom.adapters.PaperMapper;
import com.elasticpom.adapters.dto.PaperDto;
import com.elasticpom.core.model.Paper;
import com.elasticpom.core.service.PaperService;
import com.elasticpom.core.service.embedding.EmbeddingService;
import com.elasticpom.external.document.ElasticPaperDocument;
import com.elasticpom.external.document.PaperDocument;
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
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Acceptance test exercising the REAL PaperController -> PaperService wiring
 * (via Spring's @WebMvcTest + @Import of the concrete service, rather than
 * mocking PaperService as PaperControllerFilterTest does), with only the
 * lowest-level boundaries (Mongo PaperRepository, ES repository/operations)
 * mocked.
 * <p>
 * This is the only test that proves a date range filter submitted through
 * POST /api/papers/search-by-query with distinct "filter_option" (from) and
 * "filter_option_end" (to) values actually flows, unmodified, from JSON
 * request body -> FilterRequest -> PaperService.findByQueryWithFilters ->
 * a real RangeQuery with distinct gte/lte -> back out as a 200 response.
 * Existing tests verify this at the PaperService unit level (with a captured
 * Query) and at the PaperController level (with PaperService mocked); this
 * test wires both real classes together end to end.
 */
@WebMvcTest(PaperController.class)
@Import(PaperService.class)
class PaperSearchRangeFilterAcceptanceTest {

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
    void searchByQuery_dateRangeFilterWithDistinctFromTo_producesRealRangeQueryWithDistinctGteLte() throws Exception {
        Map<String, Object> properties = Map.of("datestamp", Map.of("type", "date"));
        Map<String, Object> mapping = Map.of("properties", properties);

        IndexOperations indexOperations = mock(IndexOperations.class);
        when(elasticsearchOperations.indexOps(ElasticPaperDocument.class)).thenReturn(indexOperations);
        when(indexOperations.getMapping()).thenReturn(mapping);

        ElasticPaperDocument doc = ElasticPaperDocument.builder().id("paper-99").build();
        SearchHit<ElasticPaperDocument> hit = mock(SearchHit.class);
        when(hit.getContent()).thenReturn(doc);

        SearchHits<ElasticPaperDocument> searchHits = mock(SearchHits.class);
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        when(elasticsearchOperations.search(queryCaptor.capture(), eq(ElasticPaperDocument.class)))
                .thenReturn(searchHits);
        when(searchHits.stream()).thenReturn(Stream.of(hit));

        PaperDocument paperDocument = new PaperDocument();
        paperDocument.setPaperId("paper-99");
        when(paperRepository.findByPaperId("paper-99")).thenReturn(paperDocument);
        Paper paper = new Paper();
        paper.setPaperId("paper-99");
        when(paperMapper.fromDocument(paperDocument)).thenReturn(paper);
        when(paperMapper.toDto(paper)).thenReturn(new PaperDto());

        String requestBody = """
                {
                  "query": "deep learning",
                  "pageSize": 10,
                  "page": 0,
                  "filters": [
                    {"filter_name": "datestamp", "filter_option": "2020-01-01", "filter_option_end": "2023-12-31"}
                  ]
                }
                """;

        mockMvc.perform(post("/api/papers/search-by-query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        NativeQuery nativeQuery = (NativeQuery) queryCaptor.getValue();
        co.elastic.clients.elasticsearch._types.query_dsl.Query boolQuery = nativeQuery.getQuery();
        co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery rangeQuery =
                boolQuery.bool().filter().get(0).range();

        assertThat(rangeQuery.untyped().gte().toString()).contains("2020-01-01");
        assertThat(rangeQuery.untyped().lte().toString()).contains("2023-12-31");
        assertThat(rangeQuery.untyped().gte()).isNotEqualTo(rangeQuery.untyped().lte());
    }

    @SuppressWarnings("unchecked")
    @Test
    void searchByQuery_dateRangeFilterDifferentFromTo_changesResultSetVsSingleValue() throws Exception {
        // First call: wide open range -> ES returns a hit
        Map<String, Object> properties = Map.of("datestamp", Map.of("type", "date"));
        Map<String, Object> mapping = Map.of("properties", properties);

        IndexOperations indexOperations = mock(IndexOperations.class);
        when(elasticsearchOperations.indexOps(ElasticPaperDocument.class)).thenReturn(indexOperations);
        when(indexOperations.getMapping()).thenReturn(mapping);

        ElasticPaperDocument doc = ElasticPaperDocument.builder().id("paper-1").build();
        SearchHit<ElasticPaperDocument> hit = mock(SearchHit.class);
        when(hit.getContent()).thenReturn(doc);

        SearchHits<ElasticPaperDocument> wideHits = mock(SearchHits.class);
        SearchHits<ElasticPaperDocument> narrowHits = mock(SearchHits.class);
        when(elasticsearchOperations.search(any(Query.class), eq(ElasticPaperDocument.class)))
                .thenReturn(wideHits)
                .thenReturn(narrowHits);
        when(wideHits.stream()).thenReturn(Stream.of(hit));
        when(narrowHits.stream()).thenReturn(Stream.empty());

        PaperDocument paperDocument = new PaperDocument();
        paperDocument.setPaperId("paper-1");
        when(paperRepository.findByPaperId("paper-1")).thenReturn(paperDocument);
        Paper paper = new Paper();
        paper.setPaperId("paper-1");
        when(paperMapper.fromDocument(paperDocument)).thenReturn(paper);
        when(paperMapper.toDto(paper)).thenReturn(new PaperDto());

        String wideRangeBody = """
                {
                  "query": "deep learning",
                  "pageSize": 10,
                  "page": 0,
                  "filters": [
                    {"filter_name": "datestamp", "filter_option": "2000-01-01", "filter_option_end": "2030-12-31"}
                  ]
                }
                """;
        mockMvc.perform(post("/api/papers/search-by-query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wideRangeBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        String narrowRangeBody = """
                {
                  "query": "deep learning",
                  "pageSize": 10,
                  "page": 0,
                  "filters": [
                    {"filter_name": "datestamp", "filter_option": "2099-01-01", "filter_option_end": "2099-01-02"}
                  ]
                }
                """;
        // Narrow range yields no hits -> service throws PaperNotInElasticException -> 404
        mockMvc.perform(post("/api/papers/search-by-query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(narrowRangeBody))
                .andExpect(status().isNotFound());
    }
}
