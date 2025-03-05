package com.example.myaccountsystem.dto;

import com.example.myaccountsystem.type.TransactionResultType;
import com.example.myaccountsystem.type.TransactionType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetTransactionResponse {
    private String accountNumber;
    private TransactionType transactionType;
    private TransactionResultType transactionResult;
    private Long transactionId;
    private Long amount;
    private LocalDateTime transactedAt;
}
