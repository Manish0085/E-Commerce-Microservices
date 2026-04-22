package com.sparepartshop.product_service.exception;

public class DuplicateResourceException extends RuntimeException{

    public DuplicateResourceException(String msg) {
        super(msg);
    }

    public DuplicateResourceException(String msg, Throwable cause) {
        super(msg, cause);

    }
}
