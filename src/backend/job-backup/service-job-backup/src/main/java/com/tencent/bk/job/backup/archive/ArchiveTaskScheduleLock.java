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

package com.tencent.bk.job.backup.archive;

import com.tencent.bk.job.backup.constant.ArchiveTaskTypeEnum;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.util.ThreadUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 归档任务调度分布式锁
 */
@Slf4j
public class ArchiveTaskScheduleLock {
    private final String LOCK_KEY_PREFIX = "archive:task:schedule";
    /**
     * 锁时间（60s)
     */
    private final Long LOCK_TIME = 60 * 1000L;
    /**
     * 最小获取锁间隔时间；为了保证在分布式系统中多个节点都能均匀获取到任务，会优先让空闲的节点获取到任务
     */
    private final long MIN_ACQUIRE_LOCK_INTERVAL_MS = 1000L;

    private volatile long lastAcquireLockTimeMS = 0L;

    private final Map<ArchiveTaskTypeEnum, String> locks = new HashMap<>();

    public ArchiveTaskScheduleLock() {
    }

    public synchronized boolean lock(ArchiveTaskTypeEnum archiveTaskType) {
        while (System.currentTimeMillis() - lastAcquireLockTimeMS < MIN_ACQUIRE_LOCK_INTERVAL_MS) {
            ThreadUtils.sleep(100L);
        }
        String lockRequestId = UUID.randomUUID().toString();
        if (!LockUtils.tryGetDistributedLock(buildLockKey(archiveTaskType), lockRequestId, LOCK_TIME)) {
            log.info("Acquire job instance archive task get lock failed!");
            return false;
        } else {
            log.info("Acquire job instance archive task lock successfully!");
            this.lastAcquireLockTimeMS = System.currentTimeMillis();
            locks.put(archiveTaskType, lockRequestId);
            return true;
        }
    }

    private String buildLockKey(ArchiveTaskTypeEnum archiveTaskTypeEnum) {
        return LOCK_KEY_PREFIX + archiveTaskTypeEnum.getType();
    }

    public synchronized void unlock(ArchiveTaskTypeEnum archiveTaskType) {
        String lockRequestId = locks.get(archiveTaskType);
        LockUtils.releaseDistributedLock(buildLockKey(archiveTaskType), lockRequestId);
    }

}
