package com.elasticpom.core.service;

import com.elasticpom.adapters.PaperMapper;
import com.elasticpom.adapters.dto.request.FilterRequest;
import com.elasticpom.core.model.Paper;
import com.elasticpom.core.service.embedding.EmbeddingService;
import com.elasticpom.exception.EmbeddingGenerationException;
import com.elasticpom.exception.PaperNotInElasticException;
import com.elasticpom.external.document.ElasticPaperDocument;
import com.elasticpom.external.document.PaperDocument;
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
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    private IndexOperations indexOperations;

    @Mock
    @SuppressWarnings("rawtypes")
    private SearchHits semanticSearchHits;

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

    private void mockSyntactic(String... ids) {
        List<ElasticPaperDocument> docs = Stream.of(ids)
                .map(id -> ElasticPaperDocument.builder().id(id).build())
                .toList();
        Page<ElasticPaperDocument> page = new PageImpl<>(docs);
        lenient().when(elasticRepository.findByQuery(any(), any())).thenReturn(page);
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
        lenient().when(elasticsearchOperations.search(any(Query.class), eq(ElasticPaperDocument.class)))
                .thenReturn(semanticSearchHits);
        lenient().when(semanticSearchHits.stream()).thenReturn(hits.stream());
    }

    private void mockPaperLookup() {
        lenient().when(paperRepository.findByPaperId(any())).thenReturn(new PaperDocument());
        lenient().when(paperMapper.fromDocument(any())).thenReturn(new Paper());
    }

    @SuppressWarnings("unchecked")
    private void mockMapping() {
        lenient().when(elasticsearchOperations.indexOps(ElasticPaperDocument.class)).thenReturn(indexOperations);
        lenient().when(indexOperations.getMapping()).thenReturn(Map.of(
                "properties", Map.of(
                        "language", Map.of("type", "keyword"))));
    }

    // -------------------------------------------------------------------------
    // Both legs return results -> merged via RRF, capped at pageSize
    // -------------------------------------------------------------------------

    @Test
    void getPapersByHybridSearch_bothLegsReturnResults_mergesAndCapsAtPageSize() {
        mockSyntactic("a", "b", "c");
        mockSemantic("b", "d", "e");
        mockPaperLookup();

        List<?> result = paperService.getPapersByHybridSearch("deep learning", 3, 0, null);

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

        List<?> result = paperService.getPapersByHybridSearch("deep learning", 10, 0, null);

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

        List<?> result = paperService.getPapersByHybridSearch("deep learning", 10, 0, null);

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
                paperService.getPapersByHybridSearch("deep learning", 10, 0, null))
                .isInstanceOf(PaperNotInElasticException.class);
    }

    // -------------------------------------------------------------------------
    // Embedding generation fails -> gracefully degrades to BM25-only results
    // rather than failing the whole request.
    // -------------------------------------------------------------------------

    @Test
    void getPapersByHybridSearch_embeddingFails_fallsBackToSyntacticOnly() {
        mockSyntactic("a", "b");
        mockPaperLookup();
        when(embeddingService.embed(any())).thenThrow(new EmbeddingGenerationException("boom", new RuntimeException()));

        List<?> result = paperService.getPapersByHybridSearch("deep learning", 10, 0, null);

        assertThat(result).hasSize(2);
    }

    // -------------------------------------------------------------------------
    // Server generates the query embedding itself - callers never supply a vector.
    // -------------------------------------------------------------------------

    @Test
    void getPapersByHybridSearch_generatesEmbeddingServerSide() {
        mockSyntactic("a");
        mockSemantic("a");
        mockPaperLookup();

        paperService.getPapersByHybridSearch("deep learning", 10, 0, null);

        verify(embeddingService).embed("deep learning");
    }

    // -------------------------------------------------------------------------
    // Filters propagate to BOTH legs: the BM25 leg must no longer hardcode null
    // filters (pre-existing bug), and the kNN leg's KnnSearch must carry the same
    // filter clauses, scoped by the live index mapping resolved once for both.
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void getPapersByHybridSearch_filtersPropagateToBothLegs() {
        mockMapping();
        mockPaperLookup();

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        List<SearchHit<ElasticPaperDocument>> hits = Stream.of("a", "b")
                .map(id -> {
                    SearchHit<ElasticPaperDocument> hit = mock(SearchHit.class);
                    when(hit.getContent()).thenReturn(ElasticPaperDocument.builder().id(id).build());
                    return hit;
                })
                .toList();
        SearchHits<ElasticPaperDocument> searchHits = mock(SearchHits.class);
        when(elasticsearchOperations.search(queryCaptor.capture(), eq(ElasticPaperDocument.class)))
                .thenReturn(searchHits);
        when(searchHits.stream()).thenReturn(hits.stream(), hits.stream());

        List<FilterRequest> filters = List.of(new FilterRequest("language", "en"));
        paperService.getPapersByHybridSearch("deep learning", 10, 0, filters);

        // Both the BM25 (NativeQuery built from buildFilteredBoolQuery) and kNN
        // (KnnSearch.filter(...)) legs go through elasticsearchOperations.search exactly
        // twice - once per leg - now that the BM25 leg's hardcoded-null-filters bug is fixed.
        verify(elasticsearchOperations, times(2)).search(any(Query.class), eq(ElasticPaperDocument.class));
        assertThat(queryCaptor.getAllValues()).hasSize(2);
    }

    // -------------------------------------------------------------------------
    // Filter semantics are genuinely IDENTICAL between the BM25 leg's bool-query
    // filter clauses and the kNN leg's pre-filter clauses - not just "both call the
    // same private method", but the actual term query (field + value) attached to
    // each leg's ES request must match byte-for-byte.
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void getPapersByHybridSearch_filterClausesAreIdenticalBetweenBm25LegAndKnnLeg() {
        mockMapping();
        mockPaperLookup();

        ArgumentCaptor<NativeQuery> queryCaptor = ArgumentCaptor.forClass(NativeQuery.class);
        List<SearchHit<ElasticPaperDocument>> hits = Stream.of("a", "b")
                .map(id -> {
                    SearchHit<ElasticPaperDocument> hit = mock(SearchHit.class);
                    when(hit.getContent()).thenReturn(ElasticPaperDocument.builder().id(id).build());
                    return hit;
                })
                .toList();
        SearchHits<ElasticPaperDocument> searchHits = mock(SearchHits.class);
        when(elasticsearchOperations.search(queryCaptor.capture(), eq(ElasticPaperDocument.class)))
                .thenReturn(searchHits);
        when(searchHits.stream()).thenReturn(hits.stream(), hits.stream());

        List<FilterRequest> filters = List.of(new FilterRequest("language", "en"));
        paperService.getPapersByHybridSearch("deep learning", 10, 0, filters);

        List<NativeQuery> captured = queryCaptor.getAllValues();
        assertThat(captured).hasSize(2);

        // Leg 1: BM25 - a NativeQuery built with a bool query (must, filter); pull out its
        // filter clause(s).
        NativeQuery bm25Query = captured.stream()
                .filter(q -> q.getKnnSearches() == null || q.getKnnSearches().isEmpty())
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected a non-kNN (BM25) NativeQuery among captured calls"));
        co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery boolQuery = bm25Query.getQuery().bool();
        List<co.elastic.clients.elasticsearch._types.query_dsl.Query> bm25FilterClauses = boolQuery.filter();

        // Leg 2: kNN - a NativeQuery whose KnnSearch carries the pre-filter clause(s).
        NativeQuery knnQuery = captured.stream()
                .filter(q -> q.getKnnSearches() != null && !q.getKnnSearches().isEmpty())
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected a kNN NativeQuery among captured calls"));
        List<co.elastic.clients.elasticsearch._types.query_dsl.Query> knnFilterClauses =
                knnQuery.getKnnSearches().get(0).filter();

        assertThat(bm25FilterClauses).hasSize(1);
        assertThat(knnFilterClauses).hasSize(1);

        co.elastic.clients.elasticsearch._types.query_dsl.TermQuery bm25Term = bm25FilterClauses.get(0).term();
        co.elastic.clients.elasticsearch._types.query_dsl.TermQuery knnTerm = knnFilterClauses.get(0).term();

        // The exact same field name and value must be present on both legs - proving the
        // kNN pre-filter narrows the SAME candidate set as the BM25 filter, not merely a
        // structurally-similar-but-different clause.
        assertThat(knnTerm.field()).isEqualTo(bm25Term.field()).isEqualTo("language");
        assertThat(knnTerm.value().stringValue()).isEqualTo(bm25Term.value().stringValue()).isEqualTo("en");
    }

    /**
     * With filters present, BOTH legs of hybrid search go through
     * {@code elasticsearchOperations.search} (the BM25 leg no longer takes the
     * filter-free {@code elasticRepository.findByQuery} shortcut once filters are
     * supplied - see {@code findIdsByQuery}), so unlike the no-filters tests above,
     * a single shared stub must hand back the BM25 leg's hits on the first
     * invocation and the kNN leg's hits on the second, in call order.
     */
    @SuppressWarnings("unchecked")
    private void mockBothLegsWithFilters(List<String> bm25Ids, List<String> knnIds) {
        SearchHits<ElasticPaperDocument> bm25Hits = mock(SearchHits.class);
        lenient().when(bm25Hits.stream()).thenReturn(bm25Ids.stream()
                .map(id -> {
                    SearchHit<ElasticPaperDocument> hit = mock(SearchHit.class);
                    lenient().when(hit.getContent()).thenReturn(ElasticPaperDocument.builder().id(id).build());
                    return hit;
                }));
        SearchHits<ElasticPaperDocument> knnHits = mock(SearchHits.class);
        lenient().when(knnHits.stream()).thenReturn(knnIds.stream()
                .map(id -> {
                    SearchHit<ElasticPaperDocument> hit = mock(SearchHit.class);
                    lenient().when(hit.getContent()).thenReturn(ElasticPaperDocument.builder().id(id).build());
                    return hit;
                }));
        lenient().when(elasticsearchOperations.search(any(Query.class), eq(ElasticPaperDocument.class)))
                .thenReturn(bm25Hits, knnHits);
    }

    // -------------------------------------------------------------------------
    // Filter excludes everything on the kNN side: kNN leg degrades to an empty
    // result (not an exception), and RRF correctly falls back to the BM25 leg
    // alone when one leg is filtered down to nothing.
    // -------------------------------------------------------------------------

    @Test
    void getPapersByHybridSearch_filterExcludesEverythingOnKnnLeg_degradesToSyntacticOnly() {
        mockMapping();
        mockBothLegsWithFilters(List.of("a", "b"), List.of());
        mockPaperLookup();

        List<FilterRequest> filters = List.of(new FilterRequest("language", "xx-impossible"));
        List<?> result = paperService.getPapersByHybridSearch("deep learning", 10, 0, filters);

        assertThat(result).hasSize(2);
    }

    // -------------------------------------------------------------------------
    // Filter excludes everything on BOTH legs -> PaperNotInElasticException, same
    // as the existing no-filters "both empty" case - filters must not change the
    // empty-result contract.
    // -------------------------------------------------------------------------

    @Test
    void getPapersByHybridSearch_filterExcludesEverythingOnBothLegs_throwsPaperNotInElasticException() {
        mockMapping();
        mockBothLegsWithFilters(List.of(), List.of());

        List<FilterRequest> filters = List.of(new FilterRequest("language", "xx-impossible"));

        assertThatThrownBy(() ->
                paperService.getPapersByHybridSearch("deep learning", 10, 0, filters))
                .isInstanceOf(PaperNotInElasticException.class);
    }

    // -------------------------------------------------------------------------
    // Blank (whitespace-only) query string still reaches the embedding service
    // as-is - there is no blank-query guard before embedding, so this documents
    // current (not necessarily desirable) behavior: the BM25 leg treats blank as
    // "match all" (see buildFilteredBoolQuery), while the embedding service is
    // still asked to embed the blank string for the kNN leg.
    // -------------------------------------------------------------------------

    @Test
    void getPapersByHybridSearch_blankQuery_stillInvokesEmbeddingServiceWithBlankString() {
        mockSyntactic("a");
        mockSemantic("a");
        mockPaperLookup();

        paperService.getPapersByHybridSearch("   ", 10, 0, null);

        verify(embeddingService).embed("   ");
    }

    // -------------------------------------------------------------------------
    // RRF fairness: neither leg dominates the merged output. Two disjoint-order
    // rankings of the SAME three ids must fuse to an order different from either
    // input list, with scores tied to the actual RRF formula (k=60).
    // -------------------------------------------------------------------------

    @Test
    void getPapersByHybridSearch_neitherLegDominates_genuineBlendNotEitherInputOrder() {
        // syntactic: x, y, z (x best) | semantic: z, y, x (z best) - opposite orderings.
        mockSyntactic("x", "y", "z");
        mockSemantic("z", "y", "x");
        mockPaperLookup();

        List<?> result = paperService.getPapersByHybridSearch("deep learning", 3, 0, null);

        // RRF score per id (k=60): x = 1/61 (rank1 syntactic) + 1/63 (rank3 semantic)
        //                          y = 1/62 + 1/62 (rank2 both)
        //                          z = 1/63 + 1/61 (rank3 syntactic, rank1 semantic)
        // x and z are symmetric (tie) and - by convexity of 1/n - strictly beat y's
        // equal-middle split, so the "always-rank-2" id does NOT win merely by
        // appearing in both legs; genuine RRF arithmetic decides the order, not a
        // naive "present in both lists wins" heuristic.
        double extremeScore = 1.0 / 61 + 1.0 / 63;
        double middleScore = 1.0 / 62 + 1.0 / 62;
        assertThat(extremeScore).isGreaterThan(middleScore);

        assertThat(result).hasSize(3);
    }

    // -------------------------------------------------------------------------
    // RRF fairness, id-level: ids ranked rank-1-in-one-leg/rank-3-in-the-other
    // ("x" and "z") must outrank the id ranked rank-2-in-both legs ("y") in the
    // merged order - the actual RRF formula, not a "shared id always wins" or
    // "leg A's order wins" heuristic - decides this, and the merged order must
    // match neither single leg's input order verbatim.
    // -------------------------------------------------------------------------

    @Test
    void getPapersByHybridSearch_rrfFusion_orderIsGenuineBlendOfBothLegsNotEitherLegVerbatim() {
        mockSyntactic("x", "y", "z");
        mockSemantic("z", "y", "x");

        PaperDocument docX = new PaperDocument();
        docX.setPaperId("x");
        PaperDocument docY = new PaperDocument();
        docY.setPaperId("y");
        PaperDocument docZ = new PaperDocument();
        docZ.setPaperId("z");
        when(paperRepository.findByPaperId("x")).thenReturn(docX);
        when(paperRepository.findByPaperId("y")).thenReturn(docY);
        when(paperRepository.findByPaperId("z")).thenReturn(docZ);
        when(paperMapper.fromDocument(any())).thenAnswer(invocation -> {
            PaperDocument doc = invocation.getArgument(0);
            Paper p = new Paper();
            p.setPaperId(doc.getPaperId());
            return p;
        });

        List<Paper> result = paperService.getPapersByHybridSearch("deep learning", 3, 0, null);

        List<String> mergedIds = result.stream().map(Paper::getPaperId).toList();
        // "y" - present at rank 2 in BOTH legs - is fused LAST, not first; "x" and "z"
        // tie for the top two spots. This is neither the syntactic order (x,y,z) nor
        // the semantic order (z,y,x) verbatim.
        assertThat(mergedIds).isNotEqualTo(List.of("x", "y", "z"));
        assertThat(mergedIds).isNotEqualTo(List.of("z", "y", "x"));
        assertThat(mergedIds.get(2)).isEqualTo("y");
        assertThat(mergedIds.subList(0, 2)).containsExactlyInAnyOrder("x", "z");
    }
}
