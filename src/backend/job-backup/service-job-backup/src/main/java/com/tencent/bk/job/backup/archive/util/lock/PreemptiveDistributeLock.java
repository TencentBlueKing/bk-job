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

/**
 * 抢占锁
 */
@Slf4j
public class PreemptiveDistributeLock {

    private final StringRedisTemplate redisTemplate;

    private final String redisLockKey;

    private final HeartBeatRedisLockConfig heartBeatRedisLockConfig;

    private volatile LockResult lockResult = null;

    public PreemptiveDistributeLock(StringRedisTemplate redisTemplate,
                                    String redisLockKey,
                                    HeartBeatRedisLockConfig heartBeatRedisLockConfig) {
        this.redisTemplate = redisTemplate;
        this.redisLockKey = redisLockKey;
        this.heartBeatRedisLockConfig = heartBeatRedisLockConfig;
    }

    public synchronized boolean lock() {
        if (this.lockResult != null) {
            log.warn("[{}] Lock is held by another process: {}", redisLockKey, lockResult.getLockValue());
            return false;
        }

        String lockRequestId = LockUtil.generateLockRequestId();
        HeartBeatRedisLock redisLock = new HeartBeatRedisLock(
            redisTemplate, redisLockKey, lockRequestId, heartBeatRedisLockConfig);
        LockResult lockResult = redisLock.lock();
        if (!lockResult.isLockGotten()) {
            return false;
        }

        this.lockResult = lockResult;
        return true;
    }

    public synchronized void unlock() {
        if (this.lockResult == null) {
            log.warn("[{}] Lock is not found", redisLockKey);
            return;
        }
        try {
            lockResult.tryToRelease();
        } finally {
            this.lockResult = null;
        }
    }
}
