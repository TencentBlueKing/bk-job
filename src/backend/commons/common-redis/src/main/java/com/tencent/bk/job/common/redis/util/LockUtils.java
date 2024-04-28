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
 * 分布式锁工具类
 */
@Slf4j
public class LockUtils {

    /**
     * 分布式锁 key 的前缀
     */
    public static final String LOCK_KEY_PREFIX = "job:util:lock:";
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
        String realKey = LOCK_KEY_PREFIX + lockKey;
        Boolean result = redisTemplate.opsForValue()
            .setIfAbsent(realKey, requestId, expireTimeMillis, TimeUnit.MILLISECONDS);
        log.debug(
            "set redis key {} to {}, expireTimeMillis={}, result={}",
            realKey,
            requestId,
            expireTimeMillis,
            result
        );
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
        Long result = redisTemplate.execute(unlockScript, Collections.singletonList(LOCK_KEY_PREFIX + lockKey),
            requestId);
        return RELEASE_SUCCESS.equals(result);
    }

    /**
     * 强制释放分布式锁
     *
     * @param lockKey 锁
     * @return 是否释放成功
     */
    public static boolean forceReleaseDistributedLock(String lockKey) {
        Boolean result = redisTemplate.delete(LOCK_KEY_PREFIX + lockKey);
        if (result != null) {
            return result;
        } else {
            return false;
        }
    }

    /**
     * 尝试获取可重入锁,可重试
     *
     * @param lockKey          锁
     * @param requestId        请求ID
     * @param expireTimeMillis 锁超时时间
     * @return 是否获取成功
     */
    public static boolean tryGetReentrantLock(String lockKey, String requestId, long expireTimeMillis) {
        int remainSeconds = 30;
        while (remainSeconds > 0) {
            try {
                //可重入锁判断
                if (requestId != null && !requestId.isEmpty() && isHoldReentrantLock(lockKey, requestId)) {
                    return true;
                }
                return tryGetDistributedLock(lockKey, requestId, expireTimeMillis);
            } catch (Throwable e) {
                String realLockKey = LOCK_KEY_PREFIX + lockKey;
                if (e instanceof RedisException || e instanceof RedisSystemException) {
                    // Redis 故障
                    String errorMsg = "Get lock from redis error! lockKey: " + realLockKey + ", requestId: "
                        + requestId + "! Block 1s until redis is up";
                    log.error(errorMsg, e);
                    remainSeconds -= 1;
                    ThreadUtils.sleep(1000L);
                } else {
                    // 非Redis原因导致的异常
                    String errorMsg = "Get lock from redis error! lockKey: " + realLockKey
                        + ", requestId: " + requestId;
                    log.error(errorMsg, e);
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * 是否已持有重入锁
     */
    private static boolean isHoldReentrantLock(String lockKey, String originValue) {
        String v = redisTemplate.opsForValue().get(LOCK_KEY_PREFIX + lockKey);
        return originValue.equals(v);
    }

}
