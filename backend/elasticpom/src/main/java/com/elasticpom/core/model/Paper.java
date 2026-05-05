package com.elasticpom.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Paper {
    private String id;
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
    private List<VersionEntry> versions;
    private String updateDate;
}
