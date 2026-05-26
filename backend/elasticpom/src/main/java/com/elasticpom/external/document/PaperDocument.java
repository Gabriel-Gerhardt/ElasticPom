package com.elasticpom.external.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDate;
import java.util.List;
@Document("Paper")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaperDocument {

    @Id
    @Field("_id")
    private String id;

    @Field("paper_id")
    private String paperId;

    @Field("datestamp")
    private LocalDate datestamp;

    @Field("title")
    private String title;

    @Field("creators")
    private List<String> creators;

    @Field("subjects")
    private List<String> subjects;

    @Field("description")
    private String description;

    @Field("publisher")
    private String publisher;

    @Field("contributors")
    private List<String> contributors;

    @Field("date")
    private LocalDate date;

    @Field("type")
    private String type;

    @Field("identifier")
    private String identifier;

    @Field("source")
    private String source;

    @Field("language")
    private String language;

    @Field("relations")
    private List<String> relations;

    @Field("coverage")
    private String coverage;

    @Field("rights")
    private String rights;

    @Field("unique_fields")
    private List<UniqueFieldDocument> uniqueFields;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UniqueFieldDocument {
        @Field("name")
        private String name;

        @Field("data")
        private String data;
    }

}