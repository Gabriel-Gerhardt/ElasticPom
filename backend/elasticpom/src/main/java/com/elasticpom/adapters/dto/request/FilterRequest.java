package com.elasticpom.adapters.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FilterRequest(
        @JsonProperty("filter_name") String filterName,
        @JsonProperty("filter_option") String filterOption) {
}
