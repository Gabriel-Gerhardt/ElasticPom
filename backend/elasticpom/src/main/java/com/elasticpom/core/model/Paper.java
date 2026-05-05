package com.elasticpom.core.model;

import java.util.List;

public class Paper {
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
