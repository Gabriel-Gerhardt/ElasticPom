package com.elasticpom.adapters;

import com.elasticpom.adapters.dto.FilterDto;
import com.elasticpom.core.model.Filter;
import com.elasticpom.external.document.FilterDocument;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FilterMapper {

    Filter fromDocument(FilterDocument doc);

    FilterDto toDto(Filter filter);
}
