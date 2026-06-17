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
import com.elasticpom.exception.BadRequestException;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class PaperService {
    private static final Set<String> RANGE_TYPES = Set.of("integer", "long", "float", "double", "date");

    private final PaperRepository paperRepository;
    private final ElasticPaperRepository elasticRepository;
    private final PaperMapper paperMapper;
    private final ElasticsearchOperations elasticsearchOperations;

    public PaperService(PaperRepository paperRepository, ElasticPaperRepository elasticRepository,
                        PaperMapper paperMapper, ElasticsearchOperations elasticsearchOperations) {
        this.paperRepository = paperRepository;
        this.elasticRepository = elasticRepository;
        this.paperMapper = paperMapper;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    public List<Paper> getPapersByQuery(String query, Integer pageSize, Integer page, List<FilterRequest> filters) {
        Pageable pageable = PageRequest.of(page, pageSize);

        List<String> paperIds;
        if (filters == null || filters.isEmpty()) {
            paperIds = elasticRepository.findByQuery(query, pageable).stream()
                    .map(ElasticPaperDocument::getId).toList();
        } else {
            paperIds = findByQueryWithFilters(query, filters, pageable);
        }

        if (paperIds.isEmpty()) {
            throw new PaperNotInElasticException("There is no paper in the elastic for the page " + page + " and this query " + query);
        }
        return getPapersByIds(paperIds);
    }

    @SuppressWarnings("unchecked")
    private List<String> findByQueryWithFilters(String query, List<FilterRequest> filters, Pageable pageable) {
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
                filterClauses.add(Query.of(q -> q.term(TermQuery.of(t -> t
                        .field(filter.filterName())
                        .value(filter.filterOption())))));
            }
        }

        Query multiMatch = Query.of(q -> q.multiMatch(m -> m
                .query(query)
                .fields("title^1.5", "subjects", "description^0.8", "creators")
                .tieBreaker(0.3)));

        List<Query> mustClauses = List.of(multiMatch);
        List<Query> finalFilterClauses = filterClauses;
        Query boolQuery = Query.of(q -> q.bool(BoolQuery.of(b -> b
                .must(mustClauses)
                .filter(finalFilterClauses))));

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
    public List<String> getDistinctFilterValues(String filterName) {
        Map<String, Object> mapping = elasticsearchOperations.indexOps(ElasticPaperDocument.class).getMapping();
        Map<String, Object> properties = (Map<String, Object>) mapping.get("properties");

        if (properties == null) {
            throw new InvalidFilterException("Filter '" + filterName + "' does not exist in the index mapping");
        }
        resolveFieldMapping(properties, filterName);

        co.elastic.clients.elasticsearch._types.aggregations.Aggregation aggregation =
                co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(a -> a
                        .terms(t -> t.field(filterName).size(1000)));

        NativeQuery nativeQuery = NativeQuery.builder()
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

    public List<Paper> getPapersBySemanticSearch(String query, float[] queryVector, Integer pageSize, Integer page) {
        if (query == null) {
            throw new PaperNotInElasticException("Query cannot be null");
        }

        List<Float> vectorList = new ArrayList<>(queryVector.length);
        for (float v : queryVector) {
            vectorList.add(v);
        }

        KnnSearch knnSearch = KnnSearch.of(k -> k
                .field("embedPaper")
                .queryVector(vectorList)
                .numCandidates(pageSize * 10)
                .k(pageSize));

        NativeQuery nativeQuery = NativeQuery.builder()
                .withKnnSearches(knnSearch)
                .withPageable(PageRequest.of(page, pageSize))
                .withSourceFilter(new FetchSourceFilter(true, new String[]{"id"}, null))
                .build();

        List<String> paperIds = elasticsearchOperations.search(nativeQuery, ElasticPaperDocument.class)
                .stream()
                .map(SearchHit::getContent)
                .map(ElasticPaperDocument::getId)
                .toList();

        if (paperIds.isEmpty()) {
            throw new PaperNotInElasticException("No papers found for the semantic search query: " + query);
        }
        return getPapersByIds(paperIds);
    }

    public List<Paper> getPapersByIds(List<String> paperIds) {
        return paperIds.stream()
                .map(paperRepository::findByPaperId)
                .filter(Objects::nonNull)
                .map(paperMapper::fromDocument)
                .toList();
    }
}
