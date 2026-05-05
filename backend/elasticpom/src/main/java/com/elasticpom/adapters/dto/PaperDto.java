package com.elasticpom.adapters.dto;

import com.elasticpom.core.model.Category;
import com.elasticpom.core.model.Version;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaperDto {
    private Long id;
    private String arxivId;
    private String authors;
    private String title;
    private String comments;
    private String journalRef;
    private String doi;
    private String reportNo;
    private List<Category> categories;
    private String license;
    private String description;
    private List<Version> versions;
    private String updateDate;
}
