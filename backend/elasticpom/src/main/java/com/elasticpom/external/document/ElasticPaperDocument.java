package com.elasticpom.external.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "paper")
public class ElasticPaperDocument {

    @Id
    @Field(type = FieldType.Keyword)
    private String id;

    @Field(type = FieldType.Date)
    private LocalDate datestamp;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private List<String> creators;

    @Field(type = FieldType.Text)
    private List<String> subjects;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Text)
    private String publisher;

    @Field(type = FieldType.Text)
    private List<String> contributors;

    @Field(type = FieldType.Date)
    private LocalDate date;

    @Field(type = FieldType.Keyword)
    private String type;

    @Field(type = FieldType.Keyword)
    private String format;

    @Field(type = FieldType.Keyword)
    private String identifier;

    @Field(type = FieldType.Text)
    private String source;

    @Field(type = FieldType.Keyword)
    private String language;

    @Field(type = FieldType.Keyword)
    private List<String> relations;

    @Field(type = FieldType.Text)
    private String coverage;

    @Field(type = FieldType.Text)
    private String rights;

    @Field(type = FieldType.Dense_Vector, dims = 384)
    private float[] embedPaper;

}
