package com.elasticpom.adapters.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FilterRequest(
        @JsonProperty("filter_name") String filterName,
        @JsonProperty("filter_option") String filterOption,
        @JsonProperty("filter_option_end") String filterOptionEnd) {

    public FilterRequest(String filterName, String filterOption) {
        this(filterName, filterOption, null);
    }
}
