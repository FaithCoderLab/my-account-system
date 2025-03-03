package com.example.myaccountsystem.repository;

import com.example.myaccountsystem.entity.Account;
import com.example.myaccountsystem.entity.User;
import com.example.myaccountsystem.type.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {
    Integer countByUser(User user);

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByUser(User user);

    Optional<Account> findByUserAndAccountNumber(User user, String accountNumber);

    List<Account> findByUserAndAccountStatus(User user, AccountStatus accountStatus);

    Optional<Account> findByAccountNumberAndAccountStatus(String accountNumber, AccountStatus accountStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberWithPessimisticLock(@Param("accountNumber") String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    @Query("SELECT COUNT(a) >= :maxCount FROM Account a WHERE a.user = :user")
    boolean hasMaxAccountCount(@Param("user") User user, @Param("maxCount") int maxCount);
}
