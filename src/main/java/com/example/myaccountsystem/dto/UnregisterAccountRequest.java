package com.example.myaccountsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnregisterAccountRequest {
    @NotBlank
    private String userId;

    @NotBlank
    private String accountNumber;
}
