package com.example.myaccountsystem.controller;

import com.example.myaccountsystem.dto.CreateAccountRequest;
import com.example.myaccountsystem.dto.CreateAccountResponse;
import com.example.myaccountsystem.dto.ErrorResponse;
import com.example.myaccountsystem.exception.AccountException;
import com.example.myaccountsystem.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @ExceptionHandler(AccountException.class)
    public ResponseEntity<ErrorResponse> handleAccountException(AccountException e) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(e.getErrorCode().name())
                .errorMessage(e.getErrorCode().getDescription())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}
