/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 支持通过心跳续期的Redis锁，防止进程被杀后锁不释放
 */
@Slf4j
public class HeartBeatRedisLock {

    private final RedisTemplate<String, String> redisTemplate;
    private final String key;
    private final String value;
    private final HeartBeatRedisLockConfig config;

    public HeartBeatRedisLock(RedisTemplate<String, String> redisTemplate,
                              String key,
                              String value,
                              HeartBeatRedisLockConfig config) {
        this.redisTemplate = redisTemplate;
        this.key = key;
        this.value = value;
        this.config = config;
    }

    public HeartBeatRedisLock(RedisTemplate<String, String> redisTemplate,
                              String key,
                              String value) {
        this.redisTemplate = redisTemplate;
        this.key = key;
        this.value = value;
        this.config = HeartBeatRedisLockConfig.getDefault();
    }

    /**
     * 在不加锁的情况下查看锁定Key的当前值，如果尚未锁定，该值为null
     *
     * @return 锁定Key的当前值
     */
    public String peekLockKeyValue() {
        String realLockKey = getRealLockKey();
        return redisTemplate.opsForValue().get(realLockKey);
    }

    /**
     * 尝试对指定的Key加锁，如果加锁成功，开启心跳线程维持该锁
     *
     * @return 加锁结果
     */
    public LockResult lock() {
        boolean lockGotten;
        try {
            lockGotten = LockUtils.tryGetDistributedLock(key, value, config.getExpireTimeMillis());
            if (!lockGotten) {
                String realLockKey = getRealLockKey();
                String realLockKeyValue = redisTemplate.opsForValue().get(realLockKey);
                log.info("Get lock {} fail, already held by {}", realLockKey, realLockKeyValue);
                return LockResult.fail(realLockKeyValue);
            }
            RedisKeyHeartBeatThread heartBeatThread = startRedisKeyHeartBeatThread();
            return LockResult.succeed(value, heartBeatThread);
        } catch (Throwable t) {
            log.error("Get lock caught exception", t);
        }
        return LockResult.fail(null);
    }

    private String getRealLockKey() {
        return LockUtils.LOCK_KEY_PREFIX + key;
    }

    private RedisKeyHeartBeatThread startRedisKeyHeartBeatThread() {
        // 开一个心跳子线程，维护当前机器正在WatchResource的状态
        RedisKeyHeartBeatThread redisKeyHeartBeatThread = new RedisKeyHeartBeatThread(
            redisTemplate,
            getRealLockKey(),
            value,
            config.getExpireTimeMillis(),
            config.getPeriodMillis()
        );
        redisKeyHeartBeatThread.setName(config.getHeartBeatThreadName());
        redisKeyHeartBeatThread.start();
        return redisKeyHeartBeatThread;
    }
}
