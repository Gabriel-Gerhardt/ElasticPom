package com.elasticpom.external.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document("filters")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilterDocument {

    @Id
    @Field("filtername")
    private String filtername;

    @Field("order")
    private int order;

    @Field("type")
    private String type;
}
