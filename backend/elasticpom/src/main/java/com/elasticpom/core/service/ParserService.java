package com.elasticpom.core.service;

import com.elasticpom.external.document.ElasticPaperDocument;
import com.elasticpom.external.document.PaperDocument;
import com.elasticpom.external.integration.ElasticPaperRepository;
import com.elasticpom.external.integration.PaperRepository;
import org.springframework.stereotype.Service;

@Service
public class ParserService {
    private final ElasticPaperRepository elasticPaperRepository;
    private final PaperRepository paperRepository;

    public ParserService(ElasticPaperRepository elasticPaperRepository, PaperRepository paperRepository) {
        this.elasticPaperRepository = elasticPaperRepository;
        this.paperRepository = paperRepository;
    }

    public void saveMongoDocument(PaperDocument paperDocument) {
        paperRepository.save(paperDocument);
    }

    public void saveElasticDocument(ElasticPaperDocument elasticPaperDocument) {
        elasticPaperRepository.save(elasticPaperDocument);
    }
}
