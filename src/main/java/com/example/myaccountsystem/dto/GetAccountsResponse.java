package com.example.myaccountsystem.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetAccountsResponse {
    private String userId;
    private List<AccountDto> accounts;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AccountDto {
        private String accountNumber;
        private Long balance;
    }
}
