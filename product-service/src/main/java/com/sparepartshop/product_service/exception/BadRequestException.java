package com.sparepartshop.product_service.exception;

import java.lang.management.ThreadInfo;

public class BadRequestException extends  RuntimeException{

    public BadRequestException(String msg) {
        super(msg);

    }

    public BadRequestException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
