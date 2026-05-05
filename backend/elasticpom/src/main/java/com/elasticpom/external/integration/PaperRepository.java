package com.elasticpom.external.integration;

import com.elasticpom.external.document.PaperDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaperRepository extends MongoRepository<PaperDocument, String> {
    PaperDocument findByArxivId(String arxivId);
}
