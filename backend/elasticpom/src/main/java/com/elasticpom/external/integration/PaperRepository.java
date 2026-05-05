package com.elasticpom.external.integration;


import com.elasticpom.external.document.PaperDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PaperRepository extends MongoRepository<PaperDocument, Long> {
    @Query("{arxivId: '?0'}")
    PaperDocument findByArxivId(String arxivId);
}
