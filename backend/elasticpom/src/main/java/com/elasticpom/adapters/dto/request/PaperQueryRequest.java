package com.elasticpom.adapters.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;


public record PaperQueryRequest(

        @NotNull(message= "Query cannot be null")
        String query,
        @Min(value = 1, message = "Page size should be bigger than 0")
        @Max(value = 50, message = "Page size should be less than or equal to 50")
        Integer pageSize,
        @Min(value = 0, message = "Page number should be bigger than 0")
        Integer page
) {

}
