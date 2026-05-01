package com.ascmoda.search.domain.exception;

public class SearchDocumentNotFoundException extends RuntimeException {

    public SearchDocumentNotFoundException(String message) {
        super(message);
    }
}
