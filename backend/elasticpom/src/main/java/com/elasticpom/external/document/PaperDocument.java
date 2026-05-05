package com.elasticpom.external.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;
@Document("Paper")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaperDocument {

    @Id
    @Field("_id")
    private String id;

    @Field("arxiv_id")
    private String arxivId;

    @Field("authors")
    private String authors;

    @Field("title")
    private String title;

    @Field("comments")
    private String comments;

    @Field("journal_ref")
    private String journalRef;

    @Field("doi")
    private String doi;

    @Field("report_no")
    private String reportNo;

    @Field("categories")
    private List<CategoryDocument> categories;

    @Field("license")
    private String license;

    @Field("description")
    private String description;

    @Field("versions")
    private List<VersionEntryDocument> versions;

    @Field("update_date")
    private String updateDate;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryDocument {
        @Field("mainTopic")
        private String mainTopic;

        @Field("secondaryTopic")
        private String secondaryTopic;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VersionEntryDocument {
        @Field("version")
        private String version;

        @Field("createdAt")
        private String createdAt;
    }
}