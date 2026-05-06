package com.elasticpom.core.service;

import com.elasticpom.core.model.Paper;
import com.elasticpom.exception.PaperNotInElasticException;
import com.elasticpom.external.document.ElasticPaperDocument;
import com.elasticpom.external.integration.ElasticPaperRepository;
import com.elasticpom.external.integration.PaperRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.elasticpom.external.mapper.PaperMapper;

import java.util.List;
import java.util.Objects;

@Service
public class PaperService {
    private final PaperRepository paperRepository;
    private final ElasticPaperRepository elasticRepository;
    private final PaperMapper paperMapper;

    public PaperService(PaperRepository paperRepository, ElasticPaperRepository elasticRepository, PaperMapper paperMapper) {
        this.paperRepository = paperRepository;
        this.elasticRepository = elasticRepository;
        this.paperMapper = paperMapper;
    }
//
//    public Paper getPaperByQuery(String query) {
//        return paperRepository.findByArxivId(arxivId);
//    }

    public List<Paper> getPapersByDefaultRelevance(Integer pageSize, Integer page) {
        Pageable pageable = PageRequest.of(page, pageSize);
        List<String> arxivIds = elasticRepository.findAll(pageable).stream().map(ElasticPaperDocument::getId).toList();
        if (arxivIds.isEmpty()) {
            throw new PaperNotInElasticException("There is no paper in the elastic for the page" + page);
        }
        return arxivIds.stream()
                .map(paperRepository::findByArxivId)
                .filter(Objects::nonNull)
                .map(paperMapper::fromDocument)
                .toList();
    }
}
