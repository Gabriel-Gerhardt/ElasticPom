package com.elasticpom.external.integration;

import com.elasticpom.external.document.FilterDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FilterRepository extends MongoRepository<FilterDocument, String> {
}
