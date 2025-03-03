package com.example.myaccountsystem.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    USER_NOT_FOUND("사용자가 없습니다."),
    MAX_ACCOUNT_PER_USER_10("사용자 최대 보유 가능 계좌는 10개입니다."),
    ACCOUNT_NUMBER_ALREADY_EXISTS("해당 계좌번호는 이미 존재합니다.");

    private final String description;
}
