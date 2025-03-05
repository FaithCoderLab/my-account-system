package com.example.myaccountsystem.dto;

import com.example.myaccountsystem.type.TransactionResultType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelBalanceResponse {
    private String accountNumber;
    private TransactionResultType transactionResult;
    private Long transactionId;
    private Long amount;
    private LocalDateTime transactedAt;
}
