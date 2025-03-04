package com.example.myaccountsystem.controller;

import com.example.myaccountsystem.dto.ErrorResponse;
import com.example.myaccountsystem.dto.UseBalanceRequest;
import com.example.myaccountsystem.dto.UseBalanceResponse;
import com.example.myaccountsystem.exception.AccountException;
import com.example.myaccountsystem.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/transaction")
public class TransactionController {
    private final TransactionService transactionService;

    @PostMapping("/use")
    public ResponseEntity<UseBalanceResponse> useBalance(
            @RequestBody @Valid UseBalanceRequest request
    ) {
        return ResponseEntity.ok(transactionService.useBalance(request));
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
