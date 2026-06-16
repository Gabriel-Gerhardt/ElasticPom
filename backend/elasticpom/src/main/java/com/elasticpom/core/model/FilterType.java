package com.elasticpom.core.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum FilterType {
    OPTION("option"),
    RANGE("range");

    private final String value;

    FilterType(String value) { this.value = value; }

    @JsonValue
    public String getValue() { return value; }
}
