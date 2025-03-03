package com.example.myaccountsystem.entity;

import com.example.myaccountsystem.type.AccountStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
public class Account {
    @Id
    private String accountNumber;

    @ManyToOne
    private User user;

    private Long balance;

    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus;

    private LocalDateTime createdAt;

    private LocalDateTime unregisteredAt;
}
