package com.example.myaccountsystem.controller;

import com.example.myaccountsystem.dto.GetTransactionResponse;
import com.example.myaccountsystem.exception.AccountException;
import com.example.myaccountsystem.service.TransactionService;
import com.example.myaccountsystem.type.ErrorCode;
import com.example.myaccountsystem.type.TransactionResultType;
import com.example.myaccountsystem.type.TransactionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @MockBean
    private TransactionService transactionService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("거래 조회 성공")
    void getTransaction_Success() throws Exception {
        // given
        given(transactionService.getTransaction(anyLong()))
                .willReturn(
                        GetTransactionResponse.builder()
                                .accountNumber("1234567890")
                                .transactionType(TransactionType.USE)
                                .transactionResult(TransactionResultType.SUCCESS)
                                .transactionId(1L)
                                .amount(1000L)
                                .transactedAt(LocalDateTime.now())
                                .build()
                );

        // when, then
        mockMvc.perform(get("/api/transaction/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andExpect(jsonPath("$.transactionType").value("USE"))
                .andExpect(jsonPath("$.transactionResult").value("SUCCESS"))
                .andExpect(jsonPath("$.transactionId").value(1L))
                .andExpect(jsonPath("$.amount").value(1000L));
    }

    @Test
    @DisplayName("거래 조회 실패 - 거래 없음")
    void getTransaction_TransactionNotFound() throws Exception {
        // given
        given(transactionService.getTransaction(anyLong()))
                .willThrow(new AccountException(ErrorCode.TRANSACTION_NOT_FOUND));

        // when, then
        mockMvc.perform(get("/api/transaction/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("TRANSACTION_NOT_FOUND"))
                .andExpect(jsonPath("$.errorMessage").value("해당 거래가 존재하지 않습니다."));
    }
}