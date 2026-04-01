package com.projectexample.examsystem.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        this(4001, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
