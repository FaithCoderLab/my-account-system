package com.example.myaccountsystem.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    USER_NOT_FOUND("사용자가 없습니다."),
    MAX_ACCOUNT_PER_USER_10("사용자 최대 보유 가능 계좌는 10개입니다."),
    ACCOUNT_NUMBER_ALREADY_EXISTS("해당 계좌번호는 이미 존재합니다."),
    ACCOUNT_NOT_FOUND("계좌가 없습니다."),
    ACCOUNT_OWNER_MISMATCH("계좌 소유주와 사용자가 일치하지 않습니다."),
    ACCOUNT_ALREADY_UNREGISTERED("계좌가 이미 해지되었습니다."),
    ACCOUNT_HAS_BALANCE("잔액이 있는 계좌는 해지할 수 없습니다."),
    INVALID_REQUEST("잘못된 요청입니다."),
    ACCOUNT_TRANSACTION_LOCK("계좌가 다른 트랜잭션에 의해 잠겨 있습니다."),
    AMOUNT_EXCEED_BALANCE("거래금액이 계좌 잔액보다 큽니다."),
    TRANSACTION_NOT_FOUND("해당 거래가 존재하지 않습니다."),
    TRANSACTION_ACCOUNT_MISMATCH("이 거래는 해당 계좌에서 발생한 거래가 아닙니다."),
    CANCEL_MUST_FULLY("부분 취소는 허용되지 않습니다."),
    TOO_SMALL_AMOUNT("거래금액이 너무 작습니다."),
    TOO_LARGE_AMOUNT("거래금액이 너무 큽니다."),;

    private final String description;
}
