package com.elasticpom.exception;

public class PaperNotInElasticException extends RuntimeException {
    public PaperNotInElasticException(String message) {
        super(message);
    }
}
