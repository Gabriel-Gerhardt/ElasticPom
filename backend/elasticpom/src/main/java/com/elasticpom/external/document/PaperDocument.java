package com.elasticpom.external.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document("Paper")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaperDocument {
    @Id
    private Long id;
    private String arxivId;
    private String authors;
    private String title;
    private String comments;
    private String journalRef;
    private String doi;
    private String reportNo;
    private List<CategoryDocument> categories;
    private String license;
    private String description;
    private List<VersionEntryDocument> versions;
    private String updateDate;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryDocument {
        private String mainTopic;
        private String secondaryTopic;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VersionEntryDocument {
        private String version;
        private String createdAt;
    }
}
