package com.example.myaccountsystem.repository;

import com.example.myaccountsystem.entity.Account;
import com.example.myaccountsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Integer countByUser(User user);
    List<Account> findByUser(User user);
    Optional<Account> findByAccountNumber(String accountNumber);
}
