package com.elasticpom.external.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "arxiv")
public class ElasticPaperDocument {

    @Id
    @Field(type = FieldType.Text)
    private String id;

    @Field(type = FieldType.Text, name = "abstract")
    private String description;

    @Field(type = FieldType.Text)
    private String authors;

    @Field(type = FieldType.Keyword, name = "authors_parsed")
    private String authorsParsed;

    @Field(type = FieldType.Text)
    private String categories;

    @Field(type = FieldType.Keyword, name = "categories_parsed")
    private String categoriesParsed;

    @Field(type = FieldType.Text)
    private String comments;

    @Field(type = FieldType.Text)
    private String doi;

    @MultiField(
            mainField = @Field(type = FieldType.Text, name = "journal-ref"),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword, ignoreAbove = 256)
    )
    private String journalRef;

    @Field(type = FieldType.Text, name = "journal_ref")
    private String journalRefAlt;

    @Field(type = FieldType.Text)
    private String license;

    @MultiField(
            mainField = @Field(type = FieldType.Text, name = "report-no"),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword, ignoreAbove = 256)
    )
    private String reportNo;

    @Field(type = FieldType.Text)
    private String submitter;

    @MultiField(
            mainField = @Field(type = FieldType.Text),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword, ignoreAbove = 256)
    )
    private String title;

    @Field(type = FieldType.Text, name = "update_date")
    private String updateDate;

    @Field(type = FieldType.Nested)
    private VersionEntry version;

    @Field(type = FieldType.Object)
    private List<VersionEntry> versions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VersionEntry {

        @Field(type = FieldType.Text)
        private String created;

        @Field(type = FieldType.Text)
        private String version;
    }
}