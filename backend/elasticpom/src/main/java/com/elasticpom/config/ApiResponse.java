package com.elasticpom.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@AllArgsConstructor
@Getter
public class ApiResponse {
    Integer statusCode;
    String description;
    String message;

}
