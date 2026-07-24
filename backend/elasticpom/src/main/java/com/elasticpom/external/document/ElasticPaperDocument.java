package com.elasticpom.external.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

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

    @MultiField(
            mainField = @Field(type = FieldType.Text),
            otherFields = {
                    @InnerField(suffix = "keyword", type = FieldType.Keyword, ignoreAbove = 256)
            }
    )
    private List<String> creators;

    @MultiField(
            mainField = @Field(type = FieldType.Text),
            otherFields = {
                    @InnerField(suffix = "keyword", type = FieldType.Keyword, ignoreAbove = 256)
            }
    )
    private List<String> subjects;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Text)
    private String publisher;

    @Field(type = FieldType.Date)
    private LocalDate date;

    @Field(type = FieldType.Keyword)
    private String type;

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

    @Field(name = "embed_paper", type = FieldType.Dense_Vector, dims = 384)
    private float[] embedPaper;

}
