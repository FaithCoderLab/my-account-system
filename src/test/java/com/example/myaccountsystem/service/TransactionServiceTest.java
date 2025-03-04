package com.example.myaccountsystem.service;

import com.example.myaccountsystem.dto.UseBalanceRequest;
import com.example.myaccountsystem.dto.UseBalanceResponse;
import com.example.myaccountsystem.entity.Account;
import com.example.myaccountsystem.entity.Transaction;
import com.example.myaccountsystem.entity.User;
import com.example.myaccountsystem.exception.AccountException;
import com.example.myaccountsystem.repository.AccountRepository;
import com.example.myaccountsystem.repository.TransactionRepository;
import com.example.myaccountsystem.repository.UserRepository;
import com.example.myaccountsystem.type.AccountStatus;
import com.example.myaccountsystem.type.ErrorCode;
import com.example.myaccountsystem.type.TransactionResultType;
import com.example.myaccountsystem.type.TransactionType;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private RedisLockService redisLockService;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    @DisplayName("잔액 사용 성공")
    void useBalance_Success() {
        // given
        User user = User.builder()
                .userId("testUser")
                .name("Test User")
                .build();

        Account account = Account.builder()
                .accountNumber("1234567890")
                .user(user)
                .balance(10000L)
                .accountStatus(AccountStatus.IN_USE)
                .build();

        given(redisLockService.acquireLock(anyString(), anyLong()))
                .willReturn(true);

        given(userRepository.findById(anyString()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumberWithPessimisticLock(anyString()))
                .willReturn(Optional.of(account));

        Transaction transaction = Transaction.builder()
                .transactionId(1L)
                .transactionType(TransactionType.USE)
                .transactionResultType(TransactionResultType.SUCCESS)
                .account(account)
                .amount(1000L)
                .balanceSnapshot(10000L)
                .transactedAt(LocalDateTime.now())
                .build();

        given(transactionRepository.save(any(Transaction.class)))
                .willReturn(transaction);

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);

        // when
        UseBalanceResponse response = transactionService.useBalance(
                new UseBalanceRequest("testUser", "1234567890", 1000L)
        );

        // then
        verify(transactionRepository, times(1)).save(transactionCaptor.capture());
        verify(accountRepository, times(1)).save(accountCaptor.capture());
        verify(redisLockService, times(1)).acquireLock(eq("1234567890"), anyLong());
        verify(redisLockService, times(1)).releaseLock(eq("1234567890"));

        assertEquals(TransactionResultType.SUCCESS, response.getTransactionResult());
        assertEquals("1234567890", response.getAccountNumber());
        assertEquals(1000L, response.getAmount());

        Transaction savedTransaction = transactionCaptor.getValue();
        assertEquals(TransactionType.USE, savedTransaction.getTransactionType());
        assertEquals(TransactionResultType.SUCCESS, savedTransaction.getTransactionResultType());
        assertEquals(1000L, savedTransaction.getAmount());
        assertEquals(10000L, savedTransaction.getBalanceSnapshot());

        Account savedAccount = accountCaptor.getValue();
        assertEquals(9000L, savedAccount.getBalance());
    }

    @Test
    @DisplayName("잔액 사용 실패 - 락 획득 실패")
    void useBalance_FailToAcquireLock() {
        // given
        given(redisLockService.acquireLock(anyString(), anyLong()))
                .willReturn(false);

        // when
        AccountException exception = assertThrows(
                AccountException.class,
                () -> transactionService.useBalance(
                        new UseBalanceRequest("testUser", "1234567890", 1000L)
                )
        );

        // then
        assertEquals(ErrorCode.ACCOUNT_TRANSACTION_LOCK, exception.getErrorCode());
        verify(redisLockService, never()).releaseLock(anyString());
        verify(userRepository, never()).findById(anyString());
        verify(accountRepository, never()).findByAccountNumberWithPessimisticLock(anyString());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("잔액 사용 실패 - 사용자 없음")
    void useBalance_UserNotFound() {
        // given
        given(redisLockService.acquireLock(anyString(), anyLong()))
                .willReturn(true);

        given(userRepository.findById(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(
                AccountException.class,
                () -> transactionService.useBalance(
                        new UseBalanceRequest("testUser", "1234567890", 1000L)
                )
        );

        // then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(redisLockService, times(1)).releaseLock(anyString());
        verify(accountRepository, never()).findByAccountNumberWithPessimisticLock(anyString());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("잔액 사용 실패 - 계좌 없음")
    void useBalance_AccountNotFound() {
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
                () -> transactionService.useBalance(
                        new UseBalanceRequest("testUser", "1234567890", 1000L)
                )
        );

        // then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
        verify(redisLockService, times(1)).releaseLock(anyString());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("잔액 사용 실패 - 소유주 불일치")
    void useBalance_OwnerMismatch() {
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
                .balance(10000L)
                .accountStatus(AccountStatus.IN_USE)
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
                () -> transactionService.useBalance(
                        new UseBalanceRequest("testUser", "1234567890", 1000L)
                )
        );

        // then
        assertEquals(ErrorCode.ACCOUNT_OWNER_MISMATCH, exception.getErrorCode());
        verify(redisLockService, times(1)).releaseLock(anyString());
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("잔액 사용 실패 - 계좌 해지 상태")
    void useBalance_AccountUnregistered() {
        // given
        User user = User.builder()
                .userId("testUser")
                .name("Test User")
                .build();

        Account account = Account.builder()
                .accountNumber("1234567890")
                .user(user)
                .balance(10000L)
                .accountStatus(AccountStatus.UNREGISTERED)
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
                () -> transactionService.useBalance(
                        new UseBalanceRequest("testUser", "1234567890", 1000L)
                )
        );

        // then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
        verify(redisLockService, times(1)).releaseLock(anyString());
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("잔액 사용 실패 - 거래금액이 너무 작음")
    void useBalance_TooSmallAmount() {
        // given
        User user = User.builder()
                .userId("testUser")
                .name("Test User")
                .build();

        Account account = Account.builder()
                .accountNumber("1234567890")
                .user(user)
                .balance(10000L)
                .accountStatus(AccountStatus.IN_USE)
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
                () -> transactionService.useBalance(
                        new UseBalanceRequest("testUser", "1234567890", 5L)
                )
        );

        // then
        assertEquals(ErrorCode.TOO_SMALL_AMOUNT, exception.getErrorCode());
        verify(redisLockService, times(1)).releaseLock(anyString());
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("잔액 사용 실패 - 거래금액이 너무 큼")
    void useBalance_TooLargeAmount() {
        // given
        User user = User.builder()
                .userId("testUser")
                .name("Test User")
                .build();

        Account account = Account.builder()
                .accountNumber("1234567890")
                .user(user)
                .balance(10000L)
                .accountStatus(AccountStatus.IN_USE)
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
                () -> transactionService.useBalance(
                        new UseBalanceRequest("testUser", "1234567890", 2_000_000_000L)
                )
        );

        // then
        assertEquals(ErrorCode.TOO_LARGE_AMOUNT, exception.getErrorCode());
        verify(redisLockService, times(1)).releaseLock(anyString());
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("잔액 사용 실패 - 잔액 부족")
    void useBalance_AmountExceedBalance() {
        // given
        User user = User.builder()
                .userId("testUser")
                .name("Test User")
                .build();

        Account account = Account.builder()
                .accountNumber("1234567890")
                .user(user)
                .balance(10000L)
                .accountStatus(AccountStatus.IN_USE)
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
                () -> transactionService.useBalance(
                        new UseBalanceRequest("testUser", "1234567890", 20000L)
                )
        );

        // then
        assertEquals(ErrorCode.AMOUNT_EXCEED_BALANCE, exception.getErrorCode());
        verify(redisLockService, times(1)).releaseLock(anyString());
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(accountRepository, never()).save(any(Account.class));
    }
}