package com.example.myaccountsystem.repository;

import com.example.myaccountsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserRepository extends JpaRepository<User, String> {
}
