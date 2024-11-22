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

package com.tencent.bk.job.backup.archive.util.lock;

import com.tencent.bk.job.common.redis.util.HeartBeatRedisLock;
import com.tencent.bk.job.common.redis.util.HeartBeatRedisLockConfig;
import com.tencent.bk.job.common.redis.util.LockResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 分布式锁(自动续期)
 */
@Slf4j
public class HeartBeatRedisLocks {
    /**
     * 锁 key 前缀
     */
    private final String lockKeyPrefix;
    /**
     * Key: lockKey ; Value: 分布式锁
     */
    private final Map<String, LockResult> locks = new ConcurrentHashMap<>();


    private final StringRedisTemplate redisTemplate;

    private final HeartBeatRedisLockConfig heartBeatRedisLockConfig;

    public HeartBeatRedisLocks(String lockKeyPrefix,
                               StringRedisTemplate redisTemplate,
                               HeartBeatRedisLockConfig heartBeatRedisLockConfig) {
        this.lockKeyPrefix = lockKeyPrefix;
        this.redisTemplate = redisTemplate;
        if (heartBeatRedisLockConfig == null) {
            this.heartBeatRedisLockConfig = HeartBeatRedisLockConfig.getDefault();
        } else {
            this.heartBeatRedisLockConfig = heartBeatRedisLockConfig;
        }
    }

    public synchronized boolean lock(String lockKey) {
        String lockRequestId = LockUtil.generateLockRequestId();
        String actualLockKey = buildActualLockKey(lockKey);
        HeartBeatRedisLock redisLock =
            new HeartBeatRedisLock(redisTemplate, actualLockKey, lockRequestId, heartBeatRedisLockConfig);

        LockResult lockResult = redisLock.lock();
        if (!lockResult.isLockGotten()) {
            log.warn("Lock is held by another process: {}", lockResult.getLockValue());
            return false;
        }

        this.locks.put(lockKey, lockResult);
        return true;
    }

    private String buildActualLockKey(String lockKey) {
        return lockKeyPrefix + ":" + lockKey;
    }


    public synchronized void unlock(String lockKey) {
        LockResult lockResult = this.locks.get(lockKey);
        if (lockResult == null) {
            log.warn("RedisLock is not found, lockKey: {}", lockKey);
            return;
        }
        try {
            lockResult.tryToRelease();
        } finally {
            locks.remove(lockKey);
        }
    }

}
