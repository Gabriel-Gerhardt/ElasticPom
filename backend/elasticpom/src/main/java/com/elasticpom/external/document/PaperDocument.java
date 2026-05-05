package com.elasticpom.external.document;

import com.elasticpom.core.model.Category;
import com.elasticpom.core.model.Version;
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
    private List<Category> categories;
    private String license;
    private String description;
    private List<Version> versions;
    private String updateDate;
}
