package com.elasticpom.config;

import com.elasticpom.exception.BadRequestException;
import org.springframework.data.elasticsearch.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(ResourceNotFoundException.class)
         public ResponseEntity<ApiResponse> handleResourceNotFound(ResourceNotFoundException ex) {
            ApiResponse apiResponse = new ApiResponse(HttpStatus.NOT_FOUND.value(), "Resource was not found", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponse);
         }
         @ExceptionHandler(BadRequestException.class)
            public ResponseEntity<ApiResponse> handleBadRequest(BadRequestException ex) {
             ApiResponse apiResponse = new ApiResponse(HttpStatus.BAD_REQUEST.value(), "Request of the resource is invalid", ex.getMessage());;
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
         }

}
