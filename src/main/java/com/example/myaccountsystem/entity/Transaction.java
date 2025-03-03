package com.example.myaccountsystem.entity;

import com.example.myaccountsystem.type.TransactionResultType;
import com.example.myaccountsystem.type.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @ManyToOne
    private Account account;

    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    private TransactionResultType transactionResultType;

    private Long amount;

    private Long balanceSnapshot;

    private LocalDateTime transactedAt;
}
