package com.example.myaccountsystem.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAccountResponse {
    private String userId;
    private String accountNumber;
    private LocalDateTime createdAt;
}
