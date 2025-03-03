package com.example.myaccountsystem.service;

import com.example.myaccountsystem.dto.CreateAccountRequest;
import com.example.myaccountsystem.dto.CreateAccountResponse;
import com.example.myaccountsystem.entity.Account;
import com.example.myaccountsystem.entity.User;
import com.example.myaccountsystem.exception.AccountException;
import com.example.myaccountsystem.repository.AccountRepository;
import com.example.myaccountsystem.repository.UserRepository;
import com.example.myaccountsystem.type.AccountStatus;
import com.example.myaccountsystem.type.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

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

        accountRepository.save(account);

        return CreateAccountResponse.builder()
                .userId(user.getUserId())
                .accountNumber(accountNumber)
                .createdAt(account.getCreatedAt())
                .build();
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
        } while (accountRepository.findByAccountNumber(accountNumber).isPresent());

        return accountNumber;
    }
}
