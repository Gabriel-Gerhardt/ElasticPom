package com.elasticpom.core.service;

import com.elasticpom.adapters.PaperMapper;
import com.elasticpom.exception.InvalidFilterException;
import com.elasticpom.external.document.ElasticPaperDocument;
import com.elasticpom.external.integration.ElasticPaperRepository;
import com.elasticpom.external.integration.PaperRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Edge cases for getDistinctFilterValues / resolveFieldMapping not covered by
 * the multi-field-name tests already in PaperServiceFilterTest (which check a
 * single-dot "subjects.keyword" valid case and a single-dot unknown-subfield
 * case). Covers:
 * - filter_name with more than one dot (e.g. "a.b.c")
 * - empty-string filter_name
 * - a filter_name that exists as a Mongo-seeded Filter (e.g. "contributors.keyword")
 *   but is absent from the ES index mapping entirely (base field not in mapping at all)
 */
@ExtendWith(MockitoExtension.class)
class PaperServiceFilterOptionsEdgeCaseTest {

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

    private PaperService paperService;

    @BeforeEach
    void setUp() {
        paperService = new PaperService(paperRepository, elasticRepository, paperMapper, elasticsearchOperations);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getDistinctFilterValues_multiDottedFilterName_throwsInvalidFilterException() {
        // "a.b.c" splits on the FIRST dot into base="a", sub="b.c". Since "b.c" as a
        // literal sub-field key won't exist in fields map, this must be rejected.
        Map<String, Object> properties = Map.of(
                "a", Map.of("type", "text", "fields", Map.of("b", Map.of("type", "keyword"))));
        Map<String, Object> mapping = Map.of("properties", properties);

        when(elasticsearchOperations.indexOps(ElasticPaperDocument.class)).thenReturn(indexOperations);
        when(indexOperations.getMapping()).thenReturn(mapping);

        assertThatThrownBy(() -> paperService.getDistinctFilterValues("a.b.c"))
                .isInstanceOf(InvalidFilterException.class)
                .hasMessageContaining("a.b.c");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getDistinctFilterValues_emptyStringFilterName_throwsInvalidFilterException() {
        Map<String, Object> properties = Map.of("title", Map.of("type", "text"));
        Map<String, Object> mapping = Map.of("properties", properties);

        when(elasticsearchOperations.indexOps(ElasticPaperDocument.class)).thenReturn(indexOperations);
        when(indexOperations.getMapping()).thenReturn(mapping);

        assertThatThrownBy(() -> paperService.getDistinctFilterValues(""))
                .isInstanceOf(InvalidFilterException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getDistinctFilterValues_mongoSeededFilterNameAbsentFromEsMapping_throwsInvalidFilterException() {
        // "contributors.keyword" is a Mongo-seeded filter name (see FILTERS in the
        // ingestor), but here the ES mapping has no "contributors" property at all -
        // simulating an out-of-sync index (e.g. mapping not yet reindexed). The
        // filter-options endpoint must still reject it rather than NPE or return
        // bogus results, even though the name is "known" to the Filters collection.
        Map<String, Object> properties = Map.of("subjects", Map.of("type", "text",
                "fields", Map.of("keyword", Map.of("type", "keyword"))));
        Map<String, Object> mapping = Map.of("properties", properties);

        when(elasticsearchOperations.indexOps(ElasticPaperDocument.class)).thenReturn(indexOperations);
        when(indexOperations.getMapping()).thenReturn(mapping);

        assertThatThrownBy(() -> paperService.getDistinctFilterValues("contributors.keyword"))
                .isInstanceOf(InvalidFilterException.class)
                .hasMessageContaining("contributors.keyword");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getDistinctFilterValues_baseFieldIsNotAMap_throwsInvalidFilterException() {
        // Defensive: base field mapping value is not itself a Map (malformed/odd mapping shape).
        Map<String, Object> properties = Map.of("weird", "not-a-map");
        Map<String, Object> mapping = Map.of("properties", properties);

        when(elasticsearchOperations.indexOps(ElasticPaperDocument.class)).thenReturn(indexOperations);
        when(indexOperations.getMapping()).thenReturn(mapping);

        assertThatThrownBy(() -> paperService.getDistinctFilterValues("weird.keyword"))
                .isInstanceOf(InvalidFilterException.class)
                .hasMessageContaining("weird.keyword");
    }
}
