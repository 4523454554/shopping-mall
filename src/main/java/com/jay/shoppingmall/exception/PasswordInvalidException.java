package com.jay.shoppingmall.exception;

public class PasswordInvalidException extends RuntimeException {

    public PasswordInvalidException(final String message) {
        super(message);
    }
}
