package com.example.myaccountsystem.controller;

import com.example.myaccountsystem.dto.*;
import com.example.myaccountsystem.entity.Account;
import com.example.myaccountsystem.exception.AccountException;
import com.example.myaccountsystem.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/account")
public class AccountController {
    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<CreateAccountResponse> createAccount(
            @RequestBody @Valid CreateAccountRequest request
    ) {
        return ResponseEntity.ok(accountService.createAccount(request));
    }

    @DeleteMapping
    public ResponseEntity<UnregisterAccountResponse> unregisterAccount(
            @RequestBody @Valid UnregisterAccountRequest request
    ) {
        return ResponseEntity.ok(accountService.unregisterAccount(request));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<GetAccountsResponse> getAccounts(
            @PathVariable String userId
    ) {
        List<Account> accounts = accountService.getAccountsByUserId(userId);

        List<GetAccountsResponse.AccountDto> accountDtos = accounts.stream()
                .map(account -> GetAccountsResponse.AccountDto.builder()
                        .accountNumber(account.getAccountNumber())
                        .balance(account.getBalance())
                        .build())
                .collect(Collectors.toList());

        GetAccountsResponse response = GetAccountsResponse.builder()
                .userId(userId)
                .accounts(accountDtos)
                .build();

        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(AccountException.class)
    public ResponseEntity<ErrorResponse> handleAccountException(AccountException e) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(e.getErrorCode().name())
                .errorMessage(e.getErrorCode().getDescription())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}
