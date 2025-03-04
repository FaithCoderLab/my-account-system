package com.example.myaccountsystem.controller;

import com.example.myaccountsystem.entity.Account;
import com.example.myaccountsystem.entity.User;
import com.example.myaccountsystem.exception.AccountException;
import com.example.myaccountsystem.service.AccountService;
import com.example.myaccountsystem.type.AccountStatus;
import com.example.myaccountsystem.type.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @MockBean
    private AccountService accountService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("계좌 목록 조회 성공")
    void getAccounts_Success() throws Exception {
        // given
        String userId = "testUser";
        User user = User.builder()
                .userId(userId)
                .name("Test User")
                .createdAt(LocalDateTime.now())
                .build();

        List<Account> accounts = Arrays.asList(
                Account.builder()
                        .accountNumber("1234567890")
                        .user(user)
                        .balance(1000L)
                        .accountStatus(AccountStatus.IN_USE)
                        .createdAt(LocalDateTime.now())
                        .build(),
                Account.builder()
                        .accountNumber("0987654321")
                        .user(user)
                        .balance(2000L)
                        .accountStatus(AccountStatus.IN_USE)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        given(accountService.getAccountsByUserId(anyString()))
                .willReturn(accounts);

        // when, then
        mockMvc.perform(get("/api/account/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.accounts").isArray())
                .andExpect(jsonPath("$.accounts.length()").value(2))
                .andExpect(jsonPath("$.accounts[0].accountNumber").value("1234567890"))
                .andExpect(jsonPath("$.accounts[0].balance").value(1000))
                .andExpect(jsonPath("$.accounts[1].accountNumber").value("0987654321"))
                .andExpect(jsonPath("$.accounts[1].balance").value(2000));
    }

    @Test
    @DisplayName("계좌 목록 조회 - 계좌 없음")
    void getAccounts_NoAccounts() throws Exception {
        // given
        String userId = "testUser";

        given(accountService.getAccountsByUserId(anyString()))
                .willReturn(Collections.emptyList());

        // when, then
        mockMvc.perform(get("/api/account/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.accounts").isArray())
                .andExpect(jsonPath("$.accounts.length()").value(0));
    }

    @Test
    @DisplayName("계좌 목록 조회 실패 - 사용자 없음")
    void getAccounts_UserNotFound() throws Exception {
        // given
        String userId = "nonExistingUser";

        given(accountService.getAccountsByUserId(anyString()))
                .willThrow(new AccountException(ErrorCode.USER_NOT_FOUND));

        // when, then
        mockMvc.perform(get("/api/account/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.errorMessage").value("사용자가 없습니다."));
    }
}