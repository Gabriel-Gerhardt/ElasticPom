package com.elasticpom.adapters.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaperDto {
    private String id;
    private String arxivId;
    private String authors;
    private String title;
    private String comments;
    private String journalRef;
    private String doi;
    private String reportNo;
    private List<CategoryDto> categories;
    private String license;
    private String description;
    private List<VersionEntryDto> versions;
    private String updateDate;
}
