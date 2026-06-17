package com.elasticpom.adapters.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record FilterOptionsRequest(

        String query,

        @NotNull(message = "filter_name cannot be null")
        @JsonProperty("filter_name")
        String filterName,

        List<FilterRequest> filters
) {

}
