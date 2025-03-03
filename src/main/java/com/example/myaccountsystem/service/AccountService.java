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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final RedisLockService redisLockService;

    private static final long ACCOUNT_LOCK_TIMEOUT = 3000;

    @Transactional
    public CreateAccountResponse createAccount(CreateAccountRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new AccountException(ErrorCode.USER_NOT_FOUND));

        if (accountRepository.countByUser(user) >= 10) {
            throw new AccountException(ErrorCode.MAX_ACCOUNT_PER_USER_10);
        }

        String accountNumber = generateUniqueAccountNumber();

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .user(user)
                .balance(request.getInitialBalance())
                .accountStatus(AccountStatus.IN_USE)
                .createdAt(LocalDateTime.now())
                .build();

        Account savedAccount = accountRepository.save(account);

        return CreateAccountResponse.builder()
                .userId(user.getUserId())
                .accountNumber(accountNumber)
                .createdAt(savedAccount.getCreatedAt())
                .build();
    }

    @Transactional
    public UnregisterAccountResponse unregisterAccount(UnregisterAccountRequest request) {
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

            if (account.getAccountStatus() == AccountStatus.UNREGISTERED) {
                throw new AccountException(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED);
            }

            if (account.getBalance() > 0) {
                throw new AccountException(ErrorCode.ACCOUNT_HAS_BALANCE);
            }

            account.setAccountStatus(AccountStatus.UNREGISTERED);
            account.setUnregisteredAt(LocalDateTime.now());

            Account savedAccount = accountRepository.save(account);

            return UnregisterAccountResponse.builder()
                    .userId(user.getUserId())
                    .accountNumber(savedAccount.getAccountNumber())
                    .unregisteredAt(savedAccount.getUnregisteredAt())
                    .build();
        } finally {
            if (isLockAcquired) {
                redisLockService.releaseLock(accountNumber);
            }
        }
    }

    @Transactional
    public List<Account> getAccountsByUserId(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AccountException(ErrorCode.USER_NOT_FOUND));

        return accountRepository.findByUserAndAccountStatus(user, AccountStatus.IN_USE);
    }

    private String generateUniqueAccountNumber() {
        Random random = new Random();
        String accountNumber;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 10; i++) {
                sb.append(random.nextInt(10));
            }
            accountNumber = sb.toString();
        } while (accountRepository.existsByAccountNumber(accountNumber));

        return accountNumber;
    }
}