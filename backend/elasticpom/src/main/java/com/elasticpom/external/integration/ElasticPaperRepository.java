package com.elasticpom.external.integration;

import com.elasticpom.external.document.ElasticPaperDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.annotations.SourceFilters;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Component;

@Component
public interface ElasticPaperRepository extends ElasticsearchRepository<ElasticPaperDocument,String> {
    @SourceFilters(includes = "id")
    Page<ElasticPaperDocument> findAll(Pageable pageable);


    @SourceFilters(includes = "id")
    @Query("""
        {
          "multi_match": {
            "query": "?0",
            "fields": ["title^1.5","categories","abstract^0.3","authors"],
            "tie_breaker": 0.3
          }
        }
        """)
    Page<ElasticPaperDocument> findByQuery(String query, Pageable pageable);
}
