package com.example.myaccountsystem.config;

import com.example.myaccountsystem.entity.User;
import com.example.myaccountsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;

    @Bean
    public CommandLineRunner initializeData() {
        return args -> {
            log.info("Starting data initialization...");

            if (userRepository.count() == 0) {
                List<User> users = Arrays.asList(
                        User.builder()
                                .userId("user1")
                                .name("사용자1")
                                .createdAt(LocalDateTime.now())
                                .build(),
                        User.builder()
                                .userId("user2")
                                .name("사용자2")
                                .createdAt(LocalDateTime.now())
                                .build(),
                        User.builder()
                                .userId("user3")
                                .name("사용자3")
                                .createdAt(LocalDateTime.now())
                                .build()
                );

                userRepository.saveAll(users);
                log.info("Sample users created: {}", users.size());
            } else {
                log.info("Users already exist, skipping initialization...");
            }
        };
    }
}
