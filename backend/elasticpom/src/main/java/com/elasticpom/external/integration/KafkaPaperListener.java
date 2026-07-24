package com.elasticpom.external.integration;

import com.elasticpom.adapters.PaperMapper;
import com.elasticpom.adapters.dto.PaperDto;
import com.elasticpom.core.service.ParserService;
import com.elasticpom.external.document.ElasticPaperDocument;
import com.elasticpom.external.document.PaperDocument;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class KafkaPaperListener {
    private final ObjectMapper objectMapper;
    private final PaperMapper paperMapper;
    private final ParserService parserService;

    public KafkaPaperListener(ObjectMapper objectMapper, PaperMapper paperMapper, ParserService parserService) {
        this.objectMapper = objectMapper;
        this.paperMapper = paperMapper;
        this.parserService = parserService;
    }

    @KafkaListener(
            topics = "paper",
            groupId = "paper-service-consumer"
    )

    public void consume(String payload)  {
        PaperDto dto = objectMapper.readValue(payload, PaperDto.class);
        PaperDocument paperMongoDocument = paperMapper.fromDtoToDocument(dto);
        ElasticPaperDocument elasticPaperDocument = paperMapper.fromDtoToElasticDocument(dto);
        parserService.saveMongoDocument(paperMongoDocument);
        parserService.saveElasticDocument(elasticPaperDocument);
    }

}
