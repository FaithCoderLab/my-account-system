package com.example.myaccountsystem.service;

import com.example.myaccountsystem.dto.CreateAccountRequest;
import com.example.myaccountsystem.entity.Account;
import com.example.myaccountsystem.entity.User;
import com.example.myaccountsystem.exception.AccountException;
import com.example.myaccountsystem.repository.AccountRepository;
import com.example.myaccountsystem.repository.UserRepository;
import com.example.myaccountsystem.type.AccountStatus;
import com.example.myaccountsystem.type.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    @DisplayName("계좌 생성 성공")
    void createAccount_Success() {
        // given
        User user = User.builder()
                .userId("dami")
                .name("다미")
                .createdAt(LocalDateTime.now())
                .build();

        given(userRepository.findById(anyString()))
                .willReturn(Optional.of(user));
        given(accountRepository.countByUser(any()))
                .willReturn(0);
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);

        // when
        CreateAccountRequest request = CreateAccountRequest.builder()
                .userId("dami")
                .initialBalance(10000L)
                .build();
        accountService.createAccount(request);

        // then
        verify(accountRepository, times(1)).save(accountCaptor.capture());
        assertEquals(user, accountCaptor.getValue().getUser());
        assertEquals(10000L, accountCaptor.getValue().getBalance());
        assertEquals(AccountStatus.IN_USE, accountCaptor.getValue().getAccountStatus());
    }

    @Test
    @DisplayName("사용자가 없는 경우 실패")
    void createAccount_UserNotFound() {
        // given
        given(userRepository.findById(anyString()))
                .willReturn(Optional.empty());

        // when & then
        CreateAccountRequest request = CreateAccountRequest.builder()
                .userId("nonExistingUser")
                .initialBalance(10000L)
                .build();

        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.createAccount(request));
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌가 10개인 경우 실패")
    void createAccount_MaxAccountPerUser() {
        // given
        User user = User.builder()
                .userId("dami")
                .name("다미")
                .createdAt(LocalDateTime.now())
                .build();

        given(userRepository.findById(anyString()))
                .willReturn(Optional.of(user));
        given(accountRepository.countByUser(any()))
                .willReturn(10);

        // when & then
        CreateAccountRequest request = CreateAccountRequest.builder()
                .userId("dami")
                .initialBalance(10000L)
                .build();

        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.createAccount(request));
        assertEquals(ErrorCode.MAX_ACCOUNT_PER_USER_10, exception.getErrorCode());
    }
}