package com.elasticpom.external.integration;

import com.elasticpom.external.document.ElasticPaperDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Component;

@Component
public interface ElasticPaperRepository extends ElasticsearchRepository<ElasticPaperDocument,String> {
    Page<ElasticPaperDocument> findAll(Pageable pageable);
}
