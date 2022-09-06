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

import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.util.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ArchiveTaskLock {
    private final String ARCHIVE_LOCK_KEY_PREFIX = "JOB_EXECUTE_LOG_ARCHIVE_LOCK";
    private final Long LOCK_TIME = 24 * 3600 * 1000L;
    /**
     * 最小获取锁间隔时间；为了保证在分布式系统中多个节点都能均匀获取到任务，会优先让空闲的节点获取到任务
     */
    private final long MIN_ACQUIRE_LOCK_INTERVAL_MS = 1000L;
    private volatile long lastAcquireLockTimeMS = 0L;
    private Map<String, String> locks = new ConcurrentHashMap<>();

    private ArchiveTaskLock() {
    }

    public static ArchiveTaskLock getInstance() {
        return ArchiveTaskLock.Inner.instance;
    }

    public synchronized boolean lock(String tableName) {
        while (System.currentTimeMillis() - lastAcquireLockTimeMS < MIN_ACQUIRE_LOCK_INTERVAL_MS) {
            ThreadUtils.sleep(10L);
        }
        String lockRequestId = UUID.randomUUID().toString();
        String archiveLockKey = ARCHIVE_LOCK_KEY_PREFIX + "_" + tableName;
        if (!LockUtils.tryGetDistributedLock(archiveLockKey, lockRequestId, LOCK_TIME)) {
            log.info("Acquire archive task lock failed! tableName: {}", tableName);
            return false;
        } else {
            log.info("Acquire archive task lock successfully! tableName: {}", tableName);
            this.lastAcquireLockTimeMS = System.currentTimeMillis();
            this.locks.put(tableName, lockRequestId);
            return true;
        }

    }

    public synchronized void unlock(String tableName, String lockRequestId) {
        String archiveLockKey = ARCHIVE_LOCK_KEY_PREFIX + "_" + tableName;
        LockUtils.releaseDistributedLock(archiveLockKey, lockRequestId);
        this.locks.remove(archiveLockKey);
    }

    public synchronized void unlock(String tableName) {
        String lockRequestId = this.locks.get(tableName);
        if (StringUtils.isNotEmpty(lockRequestId)) {
            String archiveLockKey = ARCHIVE_LOCK_KEY_PREFIX + "_" + tableName;
            LockUtils.releaseDistributedLock(archiveLockKey, lockRequestId);
            this.locks.remove(archiveLockKey);
        }
    }

    public void unlockAll() {
        this.locks.forEach(this::unlock);
        this.locks.clear();
    }

    private static class Inner {
        private static final ArchiveTaskLock instance = new ArchiveTaskLock();
    }

}
