package com.example.myaccountsystem.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnregisterAccountResponse {
    private String userId;
    private String accountNumber;
    private LocalDateTime unregisteredAt;
}
