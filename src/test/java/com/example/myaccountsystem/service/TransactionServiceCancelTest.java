package com.example.myaccountsystem.service;

import com.example.myaccountsystem.dto.CancelBalanceRequest;
import com.example.myaccountsystem.dto.CancelBalanceResponse;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceCancelTest {

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
    @DisplayName("잔액 사용 취소 성공")
    void cancelBalance_Success() {
        // given
        User user = User.builder()
                .userId("testUser")
                .name("Test User")
                .build();

        Account account = Account.builder()
                .accountNumber("1234567890")
                .user(user)
                .balance(9000L)
                .accountStatus(AccountStatus.IN_USE)
                .build();

        Transaction transaction = Transaction.builder()
                .transactionId(1L)
                .transactionType(TransactionType.USE)
                .transactionResultType(TransactionResultType.SUCCESS)
                .account(account)
                .amount(1000L)
                .balanceSnapshot(10000L)
                .transactedAt(LocalDateTime.now())
                .build();

        given(redisLockService.acquireLock(anyString(), anyLong()))
                .willReturn(true);

        given(accountRepository.findByAccountNumberWithPessimisticLock(anyString()))
                .willReturn(Optional.of(account));

        given(transactionRepository.findByTransactionId(anyLong()))
                .willReturn(Optional.of(transaction));

        Transaction cancelTransaction = Transaction.builder()
                .transactionId(2L)
                .transactionType(TransactionType.CANCEL)
                .transactionResultType(TransactionResultType.SUCCESS)
                .account(account)
                .amount(1000L)
                .balanceSnapshot(10000L)
                .transactedAt(LocalDateTime.now())
                .build();

        given(transactionRepository.save(any(Transaction.class)))
                .willReturn(cancelTransaction);

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);

        // when
        CancelBalanceResponse response = transactionService.cancelBalance(
                new CancelBalanceRequest(1L, "1234567890", 1000L)
        );

        // then
        verify(transactionRepository, times(1)).save(transactionCaptor.capture());
        verify(accountRepository, times(1)).save(accountCaptor.capture());
        verify(redisLockService, times(1)).acquireLock(eq("1234567890"), anyLong());
        verify(redisLockService, times(1)).releaseLock(eq("1234567890"));

        assertEquals(TransactionResultType.SUCCESS, response.getTransactionResult());
        assertEquals("1234567890", response.getAccountNumber());
        assertEquals(1000L, response.getAmount());
        assertEquals(2L, response.getTransactionId());

        Transaction savedTransaction = transactionCaptor.getValue();
        assertEquals(TransactionType.CANCEL, savedTransaction.getTransactionType());
        assertEquals(TransactionResultType.SUCCESS, savedTransaction.getTransactionResultType());
        assertEquals(1000L, savedTransaction.getAmount());

        Account savedAccount = accountCaptor.getValue();
        assertEquals(10000L, savedAccount.getBalance());
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 트랜잭션 없음")
    void cancelBalance_TransactionNotFound() {
        // given
        given(redisLockService.acquireLock(anyString(), anyLong()))
                .willReturn(true);

        given(transactionRepository.findByTransactionId(anyLong()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(
                AccountException.class,
                () -> transactionService.cancelBalance(
                        new CancelBalanceRequest(1L, "1234567890", 1000L)
                )
        );

        // then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
        verify(redisLockService, times(1)).releaseLock(anyString());
        verify(accountRepository, never()).findByAccountNumberWithPessimisticLock(anyString());
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 계좌 없음")
    void cancelBalance_AccountNotFound() {
        // given
        User user = User.builder()
                .userId("testUser")
                .name("Test User")
                .build();

        Account account = Account.builder()
                .accountNumber("1234567890")
                .user(user)
                .balance(9000L)
                .accountStatus(AccountStatus.IN_USE)
                .build();

        Transaction transaction = Transaction.builder()
                .transactionId(1L)
                .transactionType(TransactionType.USE)
                .transactionResultType(TransactionResultType.SUCCESS)
                .account(account)
                .amount(1000L)
                .balanceSnapshot(10000L)
                .transactedAt(LocalDateTime.now())
                .build();

        given(redisLockService.acquireLock(anyString(), anyLong()))
                .willReturn(true);

        given(transactionRepository.findByTransactionId(anyLong()))
                .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumberWithPessimisticLock(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(
                AccountException.class,
                () -> transactionService.cancelBalance(
                        new CancelBalanceRequest(1L, "1234567890", 1000L)
                )
        );

        // then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
        verify(redisLockService, times(1)).releaseLock(anyString());
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 트랜잭션 계좌 불일치")
    void cancel_TransactionAccountMismatch() {
        // given
        User user = User.builder()
                .userId("testUser")
                .name("Test User")
                .build();

        Account account1 = Account.builder()
                .accountNumber("1234567890")
                .user(user)
                .balance(9000L)
                .accountStatus(AccountStatus.IN_USE)
                .build();

        Account account2 = Account.builder()
                .accountNumber("0987654321")
                .user(user)
                .balance(5000L)
                .accountStatus(AccountStatus.IN_USE)
                .build();

        Transaction transaction = Transaction.builder()
                .transactionId(1L)
                .transactionType(TransactionType.USE)
                .transactionResultType(TransactionResultType.SUCCESS)
                .account(account1)
                .amount(1000L)
                .balanceSnapshot(10000L)
                .transactedAt(LocalDateTime.now())
                .build();

        given(redisLockService.acquireLock(anyString(), anyLong()))
                .willReturn(true);

        given(transactionRepository.findByTransactionId(anyLong()))
                .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumberWithPessimisticLock("0987654321"))
                .willReturn(Optional.of(account2));

        // when
        AccountException exception = assertThrows(
                AccountException.class,
                () -> transactionService.cancelBalance(
                        new CancelBalanceRequest(1L, "0987654321", 1000L)
                )
        );

        // then
        assertEquals(ErrorCode.TRANSACTION_ACCOUNT_MISMATCH, exception.getErrorCode());
        verify(redisLockService, times(1)).releaseLock(anyString());
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 금액 불일치")
    void cancelBalance_AmountMismatch() {
        // given
        User user = User.builder()
                .userId("testUser")
                .name("Test User")
                .build();

        Account account = Account.builder()
                .accountNumber("1234567890")
                .user(user)
                .balance(9000L)
                .accountStatus(AccountStatus.IN_USE)
                .build();

        Transaction transaction = Transaction.builder()
                .transactionId(1L)
                .transactionType(TransactionType.USE)
                .transactionResultType(TransactionResultType.SUCCESS)
                .account(account)
                .amount(1000L)
                .balanceSnapshot(10000L)
                .transactedAt(LocalDateTime.now())
                .build();

        given(redisLockService.acquireLock(anyString(), anyLong()))
                .willReturn(true);

        given(transactionRepository.findByTransactionId(anyLong()))
                .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumberWithPessimisticLock(anyString()))
                .willReturn(Optional.of(account));

        // when
        AccountException exception = assertThrows(
                AccountException.class,
                () -> transactionService.cancelBalance(
                        new CancelBalanceRequest(1L, "1234567890", 500L)
                )
        );

        // then
        assertEquals(ErrorCode.CANCEL_MUST_FULLY, exception.getErrorCode());
        verify(redisLockService, times(1)).releaseLock(anyString());
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 이미 취소된 계좌")
    void cancelBalance_AlreadyCanceled() {
        // given
        User user = User.builder()
                .userId("testUser")
                .name("Test User")
                .build();

        Account account = Account.builder()
                .accountNumber("1234567890")
                .user(user)
                .balance(9000L)
                .accountStatus(AccountStatus.IN_USE)
                .build();

        Transaction transaction = Transaction.builder()
                .transactionId(1L)
                .transactionType(TransactionType.CANCEL)
                .transactionResultType(TransactionResultType.SUCCESS)
                .account(account)
                .amount(1000L)
                .balanceSnapshot(9000L)
                .transactedAt(LocalDateTime.now())
                .build();

        given(redisLockService.acquireLock(anyString(), anyLong()))
                .willReturn(true);

        given(transactionRepository.findByTransactionId(anyLong()))
                .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumberWithPessimisticLock(anyString()))
                .willReturn(Optional.of(account));

        // when
        AccountException exception = assertThrows(
                AccountException.class,
                () -> transactionService.cancelBalance(
                        new CancelBalanceRequest(1L, "1234567890", 1000L
                        )
                ));

        // then
        assertEquals(ErrorCode.TRANSACTION_ALREADY_CANCELED, exception.getErrorCode());
        verify(redisLockService, times(1)).releaseLock(anyString());
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(accountRepository, never()).save(any(Account.class));
    }
}
