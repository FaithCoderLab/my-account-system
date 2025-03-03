package com.example.myaccountsystem.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisLockService {
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String LOCK_KEY_PREFIX = "LOCK:";

    public boolean acquireLock(String key, long timeout) {
        try {
            String lockKey = LOCK_KEY_PREFIX + key;
            return Boolean.TRUE.equals(
                    redisTemplate.opsForValue().setIfAbsent(lockKey, "LOCKED", Duration.ofMillis(timeout))
            );
        } catch (Exception e) {
            log.error("Failed to acquire Redis lock for key: {}", key, e);
            return false;
        }
    }

    public void releaseLock(String key) {
        try {
            String lockKey = LOCK_KEY_PREFIX + key;
            redisTemplate.delete(lockKey);
        } catch (Exception e) {
            log.error("Failed to release Redis lock for key: {}", key, e);
        }
    }

    public boolean isLocked(String key) {
        try {
            String lockKey = LOCK_KEY_PREFIX + key;
            return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
        } catch (Exception e) {
            log.error("Failed to check Redis lock status for key: {}", key, e);
            return false;
        }
    }

    public boolean extendLock(String key, long timeout) {
        try {
            String lockKey = LOCK_KEY_PREFIX + key;
            return Boolean.TRUE.equals(
                    redisTemplate.expire(lockKey, Duration.ofMillis(timeout))
            );
        } catch (Exception e) {
            log.error("Failed to extend Redis lock for key: {}", key, e);
            return false;
        }
    }

    public boolean forceReleaseLock(String key) {
        try {
            String lockKey = LOCK_KEY_PREFIX + key;
            return Boolean.TRUE.equals(redisTemplate.delete(lockKey));
        } catch (Exception e) {
            log.error("Failed to force release Redis lock for key: {}", key, e);
            return false;
        }
    }
}