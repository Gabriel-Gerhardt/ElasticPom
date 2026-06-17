package com.elasticpom.adapters.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilterDto {
    private String filtername;
    private int order;
    private String type;
}
