package com.elasticpom.adapters.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaperDto {
    private String paperId;
    private LocalDate datestamp;
    private String title;
    private List<String> creators;
    private List<String> subjects;
    private String description;
    private String publisher;
    private List<String> contributors;
    private LocalDate date;
    private String type;
    private String identifier;
    private String source;
    private String language;
    private List<String> relations;
    private String coverage;
    private String rights;
    private List<UniqueFieldDto> uniqueFields;
}
