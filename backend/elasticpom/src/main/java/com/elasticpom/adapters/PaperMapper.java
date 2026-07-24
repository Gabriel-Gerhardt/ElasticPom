package com.elasticpom.adapters;

import com.elasticpom.adapters.dto.PaperDto;
import com.elasticpom.core.model.Paper;
import com.elasticpom.external.document.ElasticPaperDocument;
import com.elasticpom.external.document.PaperDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaperMapper {

    Paper fromDocument(PaperDocument doc);

    PaperDocument toDocument(Paper paper);

    PaperDto toDto(Paper paper);

    PaperDocument fromDtoToDocument(PaperDto dto);

    @Mapping(target = "id", source = "paperId")
    ElasticPaperDocument fromDtoToElasticDocument(PaperDto dto);
}