package com.example.myaccountsystem.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelBalanceRequest {
    @NotNull
    private Long transactionId;

    @NotBlank
    private String accountNumber;

    @NotNull
    @Min(10)
    private Long amount;
}
