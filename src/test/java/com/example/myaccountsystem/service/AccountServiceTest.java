package com.example.myaccountsystem.service;

import com.example.myaccountsystem.dto.CreateAccountRequest;
import com.example.myaccountsystem.dto.CreateAccountResponse;
import com.example.myaccountsystem.dto.UnregisterAccountRequest;
import com.example.myaccountsystem.dto.UnregisterAccountResponse;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisLockService redisLockService;

    @InjectMocks
    private AccountService accountService;

    @Test
    @DisplayName("계좌 생성 성공")
    void createAccount_Success() {
        // given
        User user = User.builder()
                .userId("testUser")
                .name("Test User")
                .build();

        given(userRepository.findById(anyString()))
                .willReturn(Optional.of(user));

        given(accountRepository.countByUser(any(User.class)))
                .willReturn(5);

        given(accountRepository.existsByAccountNumber(anyString()))
                .willReturn(false);

        Account savedAccount = Account.builder()
                .user(user)
                .balance(1000L)
                .accountStatus(AccountStatus.IN_USE)
                .createdAt(LocalDateTime.now())
                .build();

        given(accountRepository.save(any(Account.class))).willAnswer(invocation -> {
            Account accountToSave = invocation.getArgument(0);
            String generatedAccountNumber = accountToSave.getAccountNumber();

            System.out.println("Generated account number: " + generatedAccountNumber);

            savedAccount.setAccountNumber(generatedAccountNumber);
            return savedAccount;
        });

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);

        // when
        CreateAccountResponse response = accountService.createAccount(
                new CreateAccountRequest("testUser", 1000L));

        // then
        verify(accountRepository).save(accountCaptor.capture());

        System.out.println("Response account number: " + response.getAccountNumber());

        assertEquals("testUser", response.getUserId());

        assertNotNull(response.getAccountNumber(), "계좌번호는 null이 아니어야 합니다.");
        assertTrue(response.getAccountNumber().matches("\\d{10}"), "계좌번호는 10자리 숫자여야 합니다.");
        
        Account capturedAccount = accountCaptor.getValue();
        assertEquals(1000L, capturedAccount.getBalance());
        assertEquals(AccountStatus.IN_USE, capturedAccount.getAccountStatus());
    }

    @Test
    @DisplayName("계좌 생성 실패 - 사용자 없음")
    void createAccount_UserNotFound() {
        // given
        given(userRepository.findById(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(
                AccountException.class,
                () -> accountService.createAccount(new CreateAccountRequest("testUser", 1000L))
        );

        // then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("계좌 생성 실패 - 최대 계좌 수 초과")
    void createAccount_MaxAccountLimit() {
        // given
        User user = User.builder()
                .userId("testUser")
                .name("Test User")
                .build();

        given(userRepository.findById(anyString()))
                .willReturn(Optional.of(user));

        given(accountRepository.countByUser(any(User.class)))
                .willReturn(10);

        // when
        AccountException exception = assertThrows(
                AccountException.class,
                () -> accountService.createAccount(new CreateAccountRequest("testUser", 1000L))
        );

        // then
        assertEquals(ErrorCode.MAX_ACCOUNT_PER_USER_10, exception.getErrorCode());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("계좌 해지 성공")
    void unregisterAccount_Success() {
        // given
        User user = User.builder()
                .userId("testUser")
                .name("Test User")
                .build();

        Account account = Account.builder()
                .accountNumber("1234567890")
                .user(user)
                .balance(0L)
                .accountStatus(AccountStatus.IN_USE)
                .createdAt(LocalDateTime.now())
                .build();

        given(userRepository.findById(anyString()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumberWithPessimisticLock(anyString()))
                .willReturn(Optional.of(account));

        given(redisLockService.acquireLock(anyString(), anyLong()))
                .willReturn(true);

        Account unregisteredAccount = Account.builder()
                .accountNumber("1234567890")
                .user(user)
                .balance(0L)
                .accountStatus(AccountStatus.UNREGISTERED)
                .createdAt(LocalDateTime.now())
                .unregisteredAt(LocalDateTime.now())
                .build();

        given(accountRepository.save(any(Account.class)))
                .willReturn(unregisteredAccount);

        ArgumentCaptor<Account> accountArgumentCaptor = ArgumentCaptor.forClass(Account.class);

        // when
        UnregisterAccountResponse response = accountService.unregisterAccount(
                new UnregisterAccountRequest("testUser", "1234567890"));

        // then
        verify(accountRepository, times(1)).save(accountArgumentCaptor.capture());
        verify(redisLockService, times(1)).acquireLock(eq("1234567890"), anyLong());
        verify(redisLockService, times(1)).releaseLock("1234567890");

        assertEquals("testUser", response.getUserId());
        assertEquals("1234567890", response.getAccountNumber());
        assertNotNull(response.getUnregisteredAt());

        Account capturedAccount = accountArgumentCaptor.getValue();
        assertEquals(AccountStatus.UNREGISTERED, capturedAccount.getAccountStatus());
        assertNotNull(capturedAccount.getUnregisteredAt());
    }

    @Test
    @DisplayName("계좌 해지 실패 - 락 획득 실패")
    void unregisterAccount_LockAcquisitionFailed() {
        // given
        given(redisLockService.acquireLock(anyString(), anyLong()))
                .willReturn(false);

        // when
        AccountException exception = assertThrows(
                AccountException.class,
                () -> accountService.unregisterAccount(new UnregisterAccountRequest("testUser", "1234567890"))
        );

        // then
        assertEquals(ErrorCode.ACCOUNT_TRANSACTION_LOCK, exception.getErrorCode());
        verify(redisLockService, never()).releaseLock(anyString());
        verify(accountRepository, never()).findByAccountNumberWithPessimisticLock(anyString());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("계좌 해지 실패 - 사용자 없음")
    void unregisterAccount_UserNotFound() {
        // given
        given(redisLockService.acquireLock(anyString(), anyLong()))
                .willReturn(true);

        given(userRepository.findById(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(
                AccountException.class,
                () -> accountService.unregisterAccount(new UnregisterAccountRequest("testUser", "1234567890"))
        );

        // then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(redisLockService, times(1)).releaseLock(anyString());
        verify(accountRepository, never()).findByAccountNumberWithPessimisticLock(anyString());
    }

    @Test
    @DisplayName("계좌 해지 실패 - 계좌 없음")
    void unregisterAccount_AccountNotFound() {
        // given
        User user = User.builder()
                .userId("testUser")
                .name("Test User")
                .build();

        given(redisLockService.acquireLock(anyString(), anyLong()))
                .willReturn(true);

        given(userRepository.findById(anyString()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumberWithPessimisticLock(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(
                AccountException.class,
                () -> accountService.unregisterAccount(new UnregisterAccountRequest("testUser", "1234567890"))
        );

        // then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
        verify(redisLockService, times(1)).releaseLock(anyString());
    }

    @Test
    @DisplayName("계좌 해지 실패 - 소유자 불일치")
    void unregisterAccount_OwnerMismatch() {
        // given
        User user1 = User.builder()
                .userId("testUser")
                .name("Test User")
                .build();

        User user2 = User.builder()
                .userId("anotherUser")
                .name("Another User")
                .build();

        Account account = Account.builder()
                .accountNumber("1234567890")
                .user(user2)
                .balance(0L)
                .accountStatus(AccountStatus.IN_USE)
                .createdAt(LocalDateTime.now())
                .build();

        given(redisLockService.acquireLock(anyString(), anyLong()))
                .willReturn(true);

        given(userRepository.findById("testUser"))
                .willReturn(Optional.of(user1));

        given(accountRepository.findByAccountNumberWithPessimisticLock(anyString()))
                .willReturn(Optional.of(account));

        // when
        AccountException exception = assertThrows(
                AccountException.class,
                () -> accountService.unregisterAccount(new UnregisterAccountRequest("testUser", "1234567890"))
        );

        // then
        assertEquals(ErrorCode.ACCOUNT_OWNER_MISMATCH, exception.getErrorCode());
        verify(redisLockService, times(1)).releaseLock(anyString());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("계좌 해지 실패 - 이미 해지된 계좌")
    void unregisterAccount_AlreadyUnregistered() {
        // given
        User user = User.builder()
                .userId("testUser")
                .name("Test User")
                .build();

        Account account = Account.builder()
                .accountNumber("1234567890")
                .user(user)
                .balance(0L)
                .accountStatus(AccountStatus.UNREGISTERED)
                .createdAt(LocalDateTime.now())
                .unregisteredAt(LocalDateTime.now())
                .build();

        given(redisLockService.acquireLock(anyString(), anyLong()))
                .willReturn(true);

        given(userRepository.findById(anyString()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumberWithPessimisticLock(anyString()))
                .willReturn(Optional.of(account));

        // when
        AccountException exception = assertThrows(
                AccountException.class,
                () -> accountService.unregisterAccount(new UnregisterAccountRequest("testUser", "1234567890"))
        );

        // then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
        verify(redisLockService, times(1)).releaseLock(anyString());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("계좌 해지 실패 - 잔액이 있는 계좌")
    void unregisterAccount_AccountHasBalance() {
        // given
        User user = User.builder()
                .userId("testUser")
                .name("Test User")
                .build();

        Account account = Account.builder()
                .accountNumber("1234567890")
                .user(user)
                .balance(1000L)
                .accountStatus(AccountStatus.IN_USE)
                .createdAt(LocalDateTime.now())
                .build();

        given(redisLockService.acquireLock(anyString(), anyLong()))
                .willReturn(true);

        given(userRepository.findById(anyString()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumberWithPessimisticLock(anyString()))
                .willReturn(Optional.of(account));

        // when
        AccountException exception = assertThrows(
                AccountException.class,
                () -> accountService.unregisterAccount(new UnregisterAccountRequest("testUser", "1234567890"))
        );

        // then
        assertEquals(ErrorCode.ACCOUNT_HAS_BALANCE, exception.getErrorCode());
        verify(redisLockService, times(1)).releaseLock(anyString());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("사용자 계좌 목록 조회 성공")
    void getAccountsByUserId_Success() {
        // given
        User user = User.builder()
                .userId("testUser")
                .name("Test User")
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

        given(userRepository.findById(anyString()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByUserAndAccountStatus(any(User.class), any(AccountStatus.class)))
                .willReturn(accounts);

        // when
        List<Account> result = accountService.getAccountsByUserId("testUser");

        // then
        assertEquals(2, result.size());
        assertEquals("1234567890", result.get(0).getAccountNumber());
        assertEquals("0987654321", result.get(1).getAccountNumber());
    }

    @Test
    @DisplayName("사용자 계좌 목록 조회 실패 - 사용자 없음")
    void getAccountsByUserId_UserNotFound() {
        // given
        given(userRepository.findById(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(
                AccountException.class,
                () -> accountService.getAccountsByUserId("testUser")
        );

        // then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(accountRepository, never()).findByUserAndAccountStatus(any(), any());
    }
}