package com.example.myaccountsystem.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class User {
    @Id
    private String userId;

    private String name;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "user")
    private List<Account> accounts = new ArrayList<>();
}
