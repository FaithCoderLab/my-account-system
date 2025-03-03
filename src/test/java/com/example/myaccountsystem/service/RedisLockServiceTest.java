package com.example.myaccountsystem.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisLockServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RedisLockService redisLockService;

    @Test
    @DisplayName("Redis 락 획득 성공")
    void acquireLock_Success() {
        String testKey = "testKey";
        long timeout = 1000L;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        boolean result = redisLockService.acquireLock(testKey, timeout);

        assertTrue(result);
        verify(valueOperations).setIfAbsent(eq("LOCK:" + testKey), eq("LOCKED"), eq(Duration.ofMillis(timeout)));
    }

    @Test
    @DisplayName("Redis 락 획득 실패")
    void acquireLock_Fail() {
        String testKey = "testKey";
        long timeout = 1000L;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        boolean result = redisLockService.acquireLock(testKey, timeout);

        assertFalse(result);
    }

    @Test
    @DisplayName("Redis 락 획득 중 예외 발생")
    void acquireLock_Exception() {
        String testKey = "testKey";
        long timeout = 1000L;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenThrow(new RuntimeException("Redis connection error"));

        boolean result = redisLockService.acquireLock(testKey, timeout);

        assertFalse(result);
    }

    @Test
    @DisplayName("Redis 락 해제 성공")
    void releaseLock_Success() {
        String testKey = "testKey";

        redisLockService.releaseLock(testKey);

        verify(redisTemplate).delete("LOCK:" + testKey);
    }

    @Test
    @DisplayName("Redis 락 상태 확인")
    void isLocked_Success() {
        String testKey = "testKey";

        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        boolean result = redisLockService.isLocked(testKey);

        assertTrue(result);
        verify(redisTemplate).hasKey("LOCK:" + testKey);
    }

    @Test
    @DisplayName("Redis 락 상태 확인 - 락 없음")
    void isLocked_NotLocked() {
        String testKey = "testKey";

        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        boolean result = redisLockService.isLocked(testKey);

        assertFalse(result);
    }

    @Test
    @DisplayName("Redis 락 상태 확인 중 예외 발생")
    void isLocked_Exception() {
        String testKey = "testKey";

        when(redisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("Redis connection error"));

        boolean result = redisLockService.isLocked(testKey);

        assertFalse(result);
    }

    @Test
    @DisplayName("Redis 락 연장 성공")
    void extendLock_Success() {
        String testKey = "testKey";
        long timeout = 1000L;

        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);

        boolean result = redisLockService.extendLock(testKey, timeout);

        assertTrue(result);
        verify(redisTemplate).expire(eq("LOCK:" + testKey), eq(Duration.ofMillis(timeout)));
    }

    @Test
    @DisplayName("Redis 락 연장 실패")
    void extendLock_Fail() {
        String testKey = "testKey";
        long timeout = 1000L;

        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(false);

        boolean result = redisLockService.extendLock(testKey, timeout);

        assertFalse(result);
    }

    @Test
    @DisplayName("Redis 락 연장 중 예외 발생")
    void extendLock_Exception() {
        String testKey = "testKey";
        long timeout = 1000L;

        when(redisTemplate.expire(anyString(), any(Duration.class)))
                .thenThrow(new RuntimeException("Redis connection error"));

        boolean result = redisLockService.extendLock(testKey, timeout);

        assertFalse(result);
    }

    @Test
    @DisplayName("Redis 락 강제 해제 성공")
    void forceReleaseLock_Success() {
        String testKey = "testKey";

        when(redisTemplate.delete(anyString())).thenReturn(true);

        boolean result = redisLockService.forceReleaseLock(testKey);

        assertTrue(result);
        verify(redisTemplate).delete("LOCK:" + testKey);
    }

    @Test
    @DisplayName("Redis 락 강제 해제 실패")
    void forceReleaseLock_Fail() {
        String testKey = "testKey";

        when(redisTemplate.delete(anyString())).thenReturn(false);

        boolean result = redisLockService.forceReleaseLock(testKey);

        assertFalse(result);
    }

    @Test
    @DisplayName("Redis 락 강제 해제 중 예외 발생")
    void forceReleaseLock_Exception() {
        String testKey = "testKey";

        when(redisTemplate.delete(anyString()))
                .thenThrow(new RuntimeException("Redis connection error"));

        boolean result = redisLockService.forceReleaseLock(testKey);

        assertFalse(result);
    }
}