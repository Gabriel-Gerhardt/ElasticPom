package com.elasticpom.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Paper {
    private String id;
    private String paperId;
    private LocalDate datestamp;
    private List<String> creators;
    private List<String> subjects;
    private String description;
    private String publisher;
    private List<String> contributors;
    private LocalDate date;
    private String type;
    private String format;
    private String identifier;
    private String source;
    private String language;
    private List<String> relations;
    private String coverage;
    private String rights;
    private String paperType;
    private List<UniqueField> uniqueFields;
}
