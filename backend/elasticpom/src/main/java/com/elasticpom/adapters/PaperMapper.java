package com.elasticpom.external.mapper;

import com.elasticpom.adapters.dto.PaperDto;
import com.elasticpom.core.model.Paper;
import com.elasticpom.external.document.PaperDocument;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaperMapper {

    Paper fromDocument(PaperDocument doc);

    PaperDocument toDocument(Paper paper);

    PaperDto toDto(Paper paper);

    Paper fromDto(PaperDto dto);
}