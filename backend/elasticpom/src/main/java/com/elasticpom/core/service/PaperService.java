package com.elasticpom.core.service;

import co.elastic.clients.elasticsearch._types.KnnSearch;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import com.elasticpom.adapters.PaperMapper;
import com.elasticpom.adapters.dto.request.FilterRequest;
import com.elasticpom.core.model.Paper;
import com.elasticpom.core.service.embedding.EmbeddingService;
import com.elasticpom.exception.BadRequestException;
import com.elasticpom.exception.EmbeddingGenerationException;
import com.elasticpom.exception.InvalidFilterException;
import com.elasticpom.exception.PaperNotInElasticException;
import com.elasticpom.external.document.ElasticPaperDocument;
import com.elasticpom.external.integration.ElasticPaperRepository;
import com.elasticpom.external.integration.PaperRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.Aggregation;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.AggregationsContainer;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
public class PaperService {
    private static final Set<String> RANGE_TYPES = Set.of("integer", "long", "float", "double", "date");

    private final PaperRepository paperRepository;
    private final ElasticPaperRepository elasticRepository;
    private final PaperMapper paperMapper;
    private final ElasticsearchOperations elasticsearchOperations;
    private final EmbeddingService embeddingService;

    public PaperService(PaperRepository paperRepository, ElasticPaperRepository elasticRepository,
                        PaperMapper paperMapper, ElasticsearchOperations elasticsearchOperations,
                        EmbeddingService embeddingService) {
        this.paperRepository = paperRepository;
        this.elasticRepository = elasticRepository;
        this.paperMapper = paperMapper;
        this.elasticsearchOperations = elasticsearchOperations;
        this.embeddingService = embeddingService;
    }

    public List<Paper> getPapersByQuery(String query, Integer pageSize, Integer page, List<FilterRequest> filters) {
        List<String> paperIds = findIdsByQuery(query, pageSize, page, filters);

        if (paperIds.isEmpty()) {
            throw new PaperNotInElasticException("There is no paper in the elastic for the page " + page + " and this query " + query);
        }
        return getPapersByIds(paperIds);
    }

    private List<String> findIdsByQuery(String query, Integer pageSize, Integer page, List<FilterRequest> filters) {
        Pageable pageable = PageRequest.of(page, pageSize);
        if (filters == null || filters.isEmpty()) {
            return elasticRepository.findByQuery(query, pageable).stream()
                    .map(ElasticPaperDocument::getId).toList();
        }
        return findByQueryWithFilters(query, filters, pageable);
    }

    private List<String> findByQueryWithFilters(String query, List<FilterRequest> filters, Pageable pageable) {
        Query boolQuery = buildFilteredBoolQuery(query, filters);

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(boolQuery)
                .withPageable(pageable)
                .withSourceFilter(new FetchSourceFilter(true, new String[]{"id"}, null))
                .build();

        return elasticsearchOperations.search(nativeQuery, ElasticPaperDocument.class)
                .stream()
                .map(SearchHit::getContent)
                .map(ElasticPaperDocument::getId)
                .toList();
    }

    /**
     * Builds the same bool query (multi-match + filter clauses) used by both
     * {@code getPapersByQuery} and {@code getDistinctFilterValues}, so the filter-options
     * aggregation runs against the exact same scoped result set as the search itself.
     * A blank/null query is treated as "match everything" (no multi-match clause) so that
     * the aggregation can also be scoped purely by filters with no free-text query.
     */
    @SuppressWarnings("unchecked")
    private Query buildFilteredBoolQuery(String query, List<FilterRequest> filters) {
        List<Query> filterClauses = buildFilterClauses(filters);

        List<Query> mustClauses;
        if (query == null || query.isBlank()) {
            mustClauses = List.of(Query.of(q -> q.matchAll(m -> m)));
        } else {
            mustClauses = List.of(Query.of(q -> q.multiMatch(m -> m
                    .query(query)
                    .fields("title^1.5", "subjects", "description^0.8", "creators")
                    .tieBreaker(0.3))));
        }

        List<Query> finalFilterClauses = filterClauses;
        return Query.of(q -> q.bool(BoolQuery.of(b -> b
                .must(mustClauses)
                .filter(finalFilterClauses))));
    }

    /**
     * Builds the list of ES filter clauses (range or term, depending on the field's mapped
     * type) for the given {@link FilterRequest}s, resolved against the live index mapping.
     * Shared by the BM25 leg (via {@link #buildFilteredBoolQuery}) and the kNN leg (via
     * {@link #findIdsBySemanticSearch}) of hybrid search, so the same filters narrow both legs
     * identically.
     */
    @SuppressWarnings("unchecked")
    private List<Query> buildFilterClauses(List<FilterRequest> filters) {
        if (filters == null || filters.isEmpty()) {
            return List.of();
        }

        Map<String, Object> mapping = elasticsearchOperations.indexOps(ElasticPaperDocument.class).getMapping();
        Map<String, Object> properties = (Map<String, Object>) mapping.get("properties");

        if (properties == null) {
            throw new InvalidFilterException("Index mapping is unavailable");
        }

        List<Query> filterClauses = new ArrayList<>();
        for (FilterRequest filter : filters) {
            Map<String, Object> fieldMapping = resolveFieldMapping(properties, filter.filterName());
            String fieldType = (String) fieldMapping.get("type");

            if (RANGE_TYPES.contains(fieldType)) {
                validateRangeValue(filter.filterName(), filter.filterOption(), fieldType);
                String rangeEnd = filter.filterOptionEnd() != null ? filter.filterOptionEnd() : filter.filterOption();
                if (filter.filterOptionEnd() != null) {
                    validateRangeValue(filter.filterName(), filter.filterOptionEnd(), fieldType);
                }
                filterClauses.add(Query.of(q -> q.range(RangeQuery.of(r -> r
                        .untyped(u -> u.field(filter.filterName())
                                .gte(co.elastic.clients.json.JsonData.of(filter.filterOption()))
                                .lte(co.elastic.clients.json.JsonData.of(rangeEnd)))))));
            } else {
                String termField = resolveTermFieldName(properties, filter.filterName());
                filterClauses.add(Query.of(q -> q.term(TermQuery.of(t -> t
                        .field(termField)
                        .value(filter.filterOption())))));
            }
        }
        return filterClauses;
    }

    /**
     * Resolves a (possibly dotted) filter name against the raw ES index mapping's
     * "properties" map and returns the mapping of the field/sub-field it refers to.
     * <p>
     * ES multi-fields (e.g. a "text" field with a "keyword" sub-field) are declared
     * as {@code properties.<base>.fields.<sub>} in the mapping, so a filter name like
     * "subjects.keyword" must be split on the dot and checked accordingly. Plain
     * names with no dot are looked up directly in {@code properties}.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveFieldMapping(Map<String, Object> properties, String filterName) {
        int dotIndex = filterName.indexOf('.');
        if (dotIndex < 0) {
            if (!properties.containsKey(filterName)) {
                throw new InvalidFilterException("Filter '" + filterName + "' does not exist in the index mapping");
            }
            return (Map<String, Object>) properties.get(filterName);
        }

        String baseName = filterName.substring(0, dotIndex);
        String subName = filterName.substring(dotIndex + 1);
        Object baseMapping = properties.get(baseName);
        if (!(baseMapping instanceof Map)) {
            throw new InvalidFilterException("Filter '" + filterName + "' does not exist in the index mapping");
        }

        Map<String, Object> baseFieldMapping = (Map<String, Object>) baseMapping;
        Object fields = baseFieldMapping.get("fields");
        if (!(fields instanceof Map) || !((Map<String, Object>) fields).containsKey(subName)) {
            throw new InvalidFilterException("Filter '" + filterName + "' does not exist in the index mapping");
        }

        return (Map<String, Object>) ((Map<String, Object>) fields).get(subName);
    }

    /**
     * Resolves the actual ES field name to use in a term query/aggregation for a logical,
     * dot-free filter name (e.g. "creators"). If the base field's mapping declares a
     * "keyword" multi-field (i.e. {@code fields.keyword}), that sub-field is targeted instead
     * of the base field, since term-level queries/aggregations need exact (not analyzed) values.
     * Callers that already pass an explicit dotted name (e.g. "subjects.keyword") are passed
     * through unchanged - resolution only kicks in for plain, dot-free names.
     */
    @SuppressWarnings("unchecked")
    private String resolveTermFieldName(Map<String, Object> properties, String filterName) {
        Map<String, Object> fieldMapping = resolveFieldMapping(properties, filterName);
        if (filterName.indexOf('.') < 0) {
            Object fields = fieldMapping.get("fields");
            if (fields instanceof Map && ((Map<String, Object>) fields).get("keyword") != null) {
                return filterName + ".keyword";
            }
        }
        return filterName;
    }

    private static final Set<String> NUMERIC_TYPES = Set.of("integer", "long", "float", "double");

    private void validateRangeValue(String fieldName, String value, String fieldType) {
        try {
            if (NUMERIC_TYPES.contains(fieldType)) {
                Double.parseDouble(value);
            } else if ("date".equals(fieldType)) {
                java.time.LocalDate.parse(value);
            }
        } catch (Exception e) {
            throw new BadRequestException("Invalid value '" + value + "' for " + fieldType + " field '" + fieldName + "'");
        }
    }

    private static final String DISTINCT_VALUES_AGG_NAME = "distinct_values";

    @SuppressWarnings("unchecked")
    public List<String> getDistinctFilterValues(String query, String filterName, List<FilterRequest> filters) {
        Map<String, Object> mapping = elasticsearchOperations.indexOps(ElasticPaperDocument.class).getMapping();
        Map<String, Object> properties = (Map<String, Object>) mapping.get("properties");

        if (properties == null) {
            throw new InvalidFilterException("Filter '" + filterName + "' does not exist in the index mapping");
        }
        String termField = resolveTermFieldName(properties, filterName);

        List<FilterRequest> scopingFilters = filters == null ? null :
                filters.stream().filter(f -> !f.filterName().equals(filterName)).toList();
        Query boolQuery = buildFilteredBoolQuery(query, scopingFilters);

        co.elastic.clients.elasticsearch._types.aggregations.Aggregation aggregation =
                co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(a -> a
                        .terms(t -> t.field(termField).size(1000)));

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(boolQuery)
                .withMaxResults(0)
                .withAggregation(DISTINCT_VALUES_AGG_NAME, aggregation)
                .build();

        SearchHits<ElasticPaperDocument> searchHits = elasticsearchOperations.search(nativeQuery, ElasticPaperDocument.class);
        AggregationsContainer<?> aggregationsContainer = searchHits.getAggregations();
        if (aggregationsContainer == null) {
            return List.of();
        }

        ElasticsearchAggregations aggregations = (ElasticsearchAggregations) aggregationsContainer;
        Aggregation namedAggregation = aggregations.aggregationsAsMap().get(DISTINCT_VALUES_AGG_NAME).aggregation();
        Aggregate aggregate = namedAggregation.getAggregate();

        if (!aggregate.isSterms()) {
            return List.of();
        }

        return aggregate.sterms().buckets().array().stream()
                .map(StringTermsBucket::key)
                .map(co.elastic.clients.elasticsearch._types.FieldValue::stringValue)
                .toList();
    }

    public List<Paper> getPapersByDefaultRelevance(Integer pageSize, Integer page) {
        Pageable pageable = PageRequest.of(page, pageSize);
        List<String> paperIds = elasticRepository.findAll(pageable).stream().map(ElasticPaperDocument::getId).toList();

        if (paperIds.isEmpty()) {
            throw new PaperNotInElasticException("There is no paper in the elastic for the page " + page);
        }
        return getPapersByIds(paperIds);
    }

    public List<Paper> getPapersBySemanticSearch(String query, Integer pageSize, Integer page) {
        if (query == null) {
            throw new PaperNotInElasticException("Query cannot be null");
        }

        float[] queryVector = embeddingService.embed(query);
        List<String> paperIds = findIdsBySemanticSearch(queryVector, pageSize, page, null);

        if (paperIds.isEmpty()) {
            throw new PaperNotInElasticException("No papers found for the semantic search query: " + query);
        }
        return getPapersByIds(paperIds);
    }

    private List<String> findIdsBySemanticSearch(float[] queryVector, Integer pageSize, Integer page, List<FilterRequest> filters) {
        log.info("Query vector length={}, first 5 values={}", queryVector.length,
                Arrays.toString(Arrays.copyOf(queryVector, Math.min(5, queryVector.length))));
        List<Float> vectorList = new ArrayList<>(queryVector.length);
        for (float v : queryVector) {
            vectorList.add(v);
        }

        List<Query> filterClauses = buildFilterClauses(filters);

        KnnSearch knnSearch = KnnSearch.of(k -> k
                .field("embed_paper")
                .queryVector(vectorList)
                .numCandidates(pageSize * 10)
                .k(pageSize)
                .filter(filterClauses));

        NativeQuery nativeQuery = NativeQuery.builder()
                .withKnnSearches(knnSearch)
                .withPageable(PageRequest.of(page, pageSize))
                .withSourceFilter(new FetchSourceFilter(true, new String[]{"id"}, null))
                .build();

        return elasticsearchOperations.search(nativeQuery, ElasticPaperDocument.class)
                .stream()
                .map(SearchHit::getContent)
                .map(ElasticPaperDocument::getId)
                .toList();
    }

    /**
     * Hybrid search: fetches up to {@code pageSize} ids from syntactic (BM25) and semantic
     * (kNN) search independently - both scoped by the same {@code filters} - then fuses the
     * two rankings with Reciprocal Rank Fusion (RRF), which compares ranks rather than raw
     * scores since BM25 and kNN scores are not on the same scale. Only a single page (page 0)
     * of each leg is fetched - this does not support multi-page re-ranking.
     * <p>
     * The query embedding is generated server-side via {@link EmbeddingService}. If embedding
     * generation fails, hybrid search degrades gracefully to BM25-only results (consistent
     * with the existing single-leg-empty fallback below) rather than failing the whole
     * request - a query embedding is "best effort" here since the syntactic leg alone can
     * still serve a useful result.
     * <p>
     * If syntactic search finds nothing, the semantic results are returned unchanged (and
     * vice versa). If both are empty, behaves like the existing single-mode searches and
     * throws {@link PaperNotInElasticException}.
     */
    public List<Paper> getPapersByHybridSearch(String query, Integer pageSize, Integer page, List<FilterRequest> filters) {
        List<String> syntacticIds = findIdsByQuery(query, pageSize, page, filters);
        log.info("Hybrid search syntactic leg for query '{}' returned {} ids", query, syntacticIds.size());

        List<String> semanticIds;
        try {
            float[] queryVector = embeddingService.embed(query);
            semanticIds = findIdsBySemanticSearch(queryVector, pageSize, page, filters);
            log.info("Hybrid search semantic leg for query '{}' returned {} ids", query, semanticIds.size());
        } catch (EmbeddingGenerationException e) {
            log.warn("Embedding generation failed, hybrid search degrading to BM25-only for query '{}'", query, e);
            semanticIds = List.of();
        }

        List<String> mergedIds;
        if (syntacticIds.isEmpty()) {
            mergedIds = semanticIds;
        } else if (semanticIds.isEmpty()) {
            mergedIds = syntacticIds;
        } else {
            mergedIds = RrfMerger.merge(syntacticIds, semanticIds, pageSize);
        }
        log.info("Hybrid search for query '{}' merged into {} ids (syntactic={}, semantic={})",
                query, mergedIds.size(), syntacticIds.size(), semanticIds.size());
        for (String id : mergedIds) {
            int syntacticRank = syntacticIds.indexOf(id);
            int semanticRank = semanticIds.indexOf(id);
            String source = syntacticRank >= 0 && semanticRank >= 0 ? "BOTH"
                    : syntacticRank >= 0 ? "SYNTACTIC" : "SEMANTIC";
            log.info("  merged id={} source={} syntacticRank={} semanticRank={}",
                    id, source, syntacticRank >= 0 ? syntacticRank + 1 : "-", semanticRank >= 0 ? semanticRank + 1 : "-");
        }

        if (mergedIds.isEmpty()) {
            throw new PaperNotInElasticException("There is no paper in the elastic for the page " + page + " and this query " + query);
        }
        return getPapersByIds(mergedIds);
    }

    public List<Paper> getPapersByIds(List<String> paperIds) {
        return paperIds.stream()
                .map(paperRepository::findByPaperId)
                .filter(Objects::nonNull)
                .map(paperMapper::fromDocument)
                .toList();
    }
}
