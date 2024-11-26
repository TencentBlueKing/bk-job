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

import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.util.ThreadUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 分布式锁（公平锁，非饥饿抢占方式)
 */
@Slf4j
public class FairDistributeLock {
    /**
     * 锁名称
     */
    private final String lockName;

    private final String lockKey;
    /**
     * 锁时间（毫秒)
     */
    private final Long lockMills;
    /**
     * 最小获取锁间隔时间
     */
    private final long MIN_ACQUIRE_LOCK_INTERVAL_MS = 1000L;

    private volatile long lastAcquireLockTimeMS = 0L;

    private volatile String lockRequestId = null;

    public FairDistributeLock(String lockName, String lockKey, long lockMills) {
        this.lockName = lockName;
        this.lockKey = lockKey;
        this.lockMills = lockMills;
    }

    public synchronized boolean lock() {
        while (System.currentTimeMillis() - lastAcquireLockTimeMS < MIN_ACQUIRE_LOCK_INTERVAL_MS) {
            // 为了保证在分布式系统中多个节点都能均匀获取到任务，需要最小获取锁间隔时间，让其他服务节点优先获取任务锁
            ThreadUtils.sleep(100L);
        }
        String lockRequestId = LockUtil.generateLockRequestId();
        if (!LockUtils.tryGetDistributedLock(lockKey, lockRequestId, lockMills)) {
            log.info("[{}] Acquire lock failed!", lockName);
            return false;
        } else {
            log.info("[{}] Acquire lock successfully!", lockName);
            this.lastAcquireLockTimeMS = System.currentTimeMillis();
            this.lockRequestId = lockRequestId;
            return true;
        }
    }


    public synchronized boolean unlock() {
        boolean success = LockUtils.releaseDistributedLock(lockKey, lockRequestId);
        if (success) {
            log.info("[{}] Release lock successfully", lockName);
            this.lockRequestId = null;
        } else {
            log.warn("[{}] Release lock fail", lockName);
        }
        return success;
    }
}
