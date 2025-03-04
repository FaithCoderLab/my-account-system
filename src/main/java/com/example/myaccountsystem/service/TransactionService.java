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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final RedisLockService redisLockService;

    private static final long MAX_TRANSACTION_AMOUNT = 1_000_000_000L;
    private static final long MIN_TRANSACTION_AMOUNT = 10L;
    private static final long ACCOUNT_LOCK_TIMEOUT = 3000;

    @Transactional
    public UseBalanceResponse useBalance(UseBalanceRequest request) {
        String accountNumber = request.getAccountNumber();
        boolean isLockAcquired = false;

        try {
            isLockAcquired = redisLockService.acquireLock(accountNumber, ACCOUNT_LOCK_TIMEOUT);

            if (!isLockAcquired) {
                throw new AccountException(ErrorCode.ACCOUNT_TRANSACTION_LOCK);
            }

            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new AccountException(ErrorCode.USER_NOT_FOUND));

            Account account = accountRepository.findByAccountNumberWithPessimisticLock(accountNumber)
                    .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

            if (!account.getUser().getUserId().equals(user.getUserId())) {
                throw new AccountException(ErrorCode.ACCOUNT_OWNER_MISMATCH);
            }

            if (account.getAccountStatus() != AccountStatus.IN_USE) {
                throw new AccountException(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED);
            }

            validateTransactionAmount(request.getAmount());

            if (account.getBalance() < request.getAmount()) {
                throw new AccountException(ErrorCode.AMOUNT_EXCEED_BALANCE);
            }

            Transaction transaction = saveTransaction(
                    account,
                    request.getAmount()
            );

            account.setBalance(account.getBalance() - request.getAmount());
            accountRepository.save(account);

            return UseBalanceResponse.builder()
                    .accountNumber(account.getAccountNumber())
                    .transactionResult(TransactionResultType.SUCCESS)
                    .transactionId(transaction.getTransactionId())
                    .amount(request.getAmount())
                    .transactedAt(transaction.getTransactedAt())
                    .build();
        } catch (AccountException e) {
            log.error("Failed to use balance: {}", e.getMessage());
            throw e;
        } finally {
            if (isLockAcquired) {
                redisLockService.releaseLock(accountNumber);
            }
        }
    }

    private void validateTransactionAmount(Long amount) {
        if (amount < MIN_TRANSACTION_AMOUNT) {
            throw new AccountException(ErrorCode.TOO_SMALL_AMOUNT);
        }

        if (amount > MAX_TRANSACTION_AMOUNT) {
            throw new AccountException(ErrorCode.TOO_LARGE_AMOUNT);
        }
    }

    private Transaction saveTransaction(
            Account account,
            Long amount
    ) {
        Transaction transaction = Transaction.builder()
                .transactionType(TransactionType.USE)
                .transactionResultType(TransactionResultType.SUCCESS)
                .account(account)
                .amount(amount)
                .balanceSnapshot(account.getBalance())
                .transactedAt(LocalDateTime.now())
                .build();

        return transactionRepository.save(transaction);
    }
}
