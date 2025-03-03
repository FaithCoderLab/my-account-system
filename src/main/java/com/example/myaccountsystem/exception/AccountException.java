package com.example.myaccountsystem.exception;

import com.example.myaccountsystem.type.ErrorCode;
import lombok.Getter;

@Getter
public class AccountException extends RuntimeException {
    private final ErrorCode errorCode;

    public AccountException(ErrorCode errorCode) {
        super(errorCode.getDescription());
        this.errorCode = errorCode;
    }
}
