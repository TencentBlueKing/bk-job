/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.tencent.bk.job.common.redis.util;

import com.tencent.bk.job.common.util.ThreadUtils;
import io.lettuce.core.RedisException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @since 20/1/2020 10:49
 */
@Slf4j
public class LockUtils {

    private static final String LOCK_PREFIX = "job:util:lock:";
    private static final Long RELEASE_SUCCESS = 1L;
    private static StringRedisTemplate redisTemplate = null;
    private static RedisScript<Long> unlockScript = null;

    /**
     * 初始化分布式锁
     *
     * @param template Spring 提供的 redis 操作模版
     * @param script   解锁脚本
     */
    public static synchronized void init(StringRedisTemplate template, RedisScript<Long> script) {
        if (redisTemplate == null) {
            redisTemplate = template;
        }
        if (unlockScript == null) {
            unlockScript = script;
        }
    }

    /**
     * 尝试获取分布式锁
     *
     * @param lockKey          锁
     * @param requestId        请求标识
     * @param expireTimeMillis 超期时间(毫秒)
     * @return 是否获取成功
     */
    public static boolean tryGetDistributedLock(String lockKey, String requestId, long expireTimeMillis) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(LOCK_PREFIX + lockKey, requestId, expireTimeMillis,
            TimeUnit.MILLISECONDS);
        if (result != null) {
            return result;
        } else {
            return false;
        }
    }

    /**
     * 尝试获取分布式锁
     *
     * @param prefix           锁前缀
     * @param lockKey          锁
     * @param requestId        请求标识
     * @param expireTimeMillis 超期时间(毫秒)
     * @return 是否获取成功
     */
    public static boolean tryGetDistributedLock(String prefix, String lockKey, String requestId,
                                                long expireTimeMillis) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(prefix + lockKey, requestId, expireTimeMillis,
            TimeUnit.MILLISECONDS);
        if (result != null) {
            return result;
        } else {
            return false;
        }
    }

    /**
     * 获取分布式锁(阻塞)
     *
     * @param lockKey          锁名称
     * @param requestId        请求标识
     * @param expireTimeMillis 锁过期时间(毫秒)
     * @param timeoutSeconds   获取锁超时时间(秒)
     * @return 是否获取成功
     */
    public static boolean lock(String lockKey, String requestId, long expireTimeMillis, int timeoutSeconds) {
        while (timeoutSeconds > 0) {
            boolean result = tryGetDistributedLock(lockKey, requestId, expireTimeMillis);
            if (!result) {
                // 等待1s
                ThreadUtils.sleep(1000L);
                timeoutSeconds--;
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * 释放分布式锁
     *
     * @param lockKey   锁
     * @param requestId 请求标识
     * @return 是否释放成功
     */
    public static boolean releaseDistributedLock(String lockKey, String requestId) {
        Long result = redisTemplate.execute(unlockScript, Collections.singletonList(LOCK_PREFIX + lockKey), requestId);
        return RELEASE_SUCCESS.equals(result);
    }

    /**
     * 释放分布式锁
     *
     * @param prefix    锁前缀
     * @param lockKey   锁
     * @param requestId 请求标识
     * @return 是否释放成功
     */
    public static boolean releaseDistributedLock(String prefix, String lockKey, String requestId) {
        Long result = redisTemplate.execute(unlockScript, Collections.singletonList(prefix + lockKey), requestId);
        return RELEASE_SUCCESS.equals(result);
    }

    /**
     * 强制释放分布式锁
     *
     * @param lockKey 锁
     * @return 是否释放成功
     */
    public static boolean forceReleaseDistributedLock(String lockKey) {
        return forceReleaseDistributedLock(LOCK_PREFIX, lockKey);
    }

    /**
     * 强制释放分布式锁
     *
     * @param prefix  锁前缀
     * @param lockKey 锁
     * @return 是否释放成功
     */
    public static boolean forceReleaseDistributedLock(String prefix, String lockKey) {
        Boolean result = redisTemplate.delete(prefix + lockKey);
        if (result != null) {
            return result;
        } else {
            return false;
        }
    }

    /**
     * 尝试获取可重入锁
     *
     * @param lockKey   锁
     * @param requestId 请求ID
     * @return 是否获取成功
     */
    public static boolean tryGetReentrantLock(String lockKey, String requestId, long expireTimeMillis) {
        int maxWaitingSeconds = 30;
        while (maxWaitingSeconds > 0) {
            try {
                //可重入锁判断
                if (requestId != null && !requestId.isEmpty() && isReentrantLock(lockKey, requestId)) {
                    return true;
                }
                return tryGetDistributedLock("", lockKey, requestId, expireTimeMillis);
            } catch (Throwable e) {
                if (e instanceof RedisException || e instanceof RedisSystemException) {
                    // Redis 故障
                    String errorMsg = "Get lock from redis error! lockKey: " + lockKey + ", requestId: " + requestId
                        + "! Block 1s until redis is up";
                    log.error(errorMsg, e);
                    maxWaitingSeconds -= 5;
                    ThreadUtils.sleep(1000L);
                } else {
                    // 非Redis原因导致的异常
                    String errorMsg = "Get lock from redis error! lockKey: " + lockKey + ", requestId: " + requestId;
                    log.error(errorMsg, e);
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * 是否为重入锁
     */
    private static boolean isReentrantLock(String key, String originValue) {
        String v = redisTemplate.opsForValue().get(key);
        return originValue.equals(v);
    }
}
