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

package com.tencent.bk.job.execute.schedule;

import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.execute.schedule.tasks.SyncAppAndRefreshCacheTask;
import com.tencent.bk.job.execute.schedule.tasks.SyncAppHostAndRefreshCacheTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@Slf4j
public class ScheduleTasks {
    private final SyncAppHostAndRefreshCacheTask syncAppHostAndRefreshCacheTask;
    private final SyncAppAndRefreshCacheTask syncAppAndRefreshCacheTask;

    @Autowired
    public ScheduleTasks(SyncAppHostAndRefreshCacheTask SyncAppHostAndRefreshCacheTask,
                         SyncAppAndRefreshCacheTask syncAppAndRefreshCacheTask) {
        this.syncAppHostAndRefreshCacheTask = SyncAppHostAndRefreshCacheTask;
        this.syncAppAndRefreshCacheTask = syncAppAndRefreshCacheTask;
    }

    @Scheduled(cron = "0 0/5 * * * ?")
    public void syncAndRefreshHost() {
        if (!LockUtils.tryGetDistributedLock("job-execute-sync-host", "sync-host", 60_000)) {
            log.info("Fail to get sync host lock!Skip sync.");
            return;
        }
        log.info("Get job-execute-sync-host lock successfully!");
        syncAppHostAndRefreshCacheTask.execute();
    }

    @Scheduled(cron = "0 * * * * ?")
    public void syncAndRefreshApp() {
        if (!LockUtils.tryGetDistributedLock("job-execute-sync-app", "sync-app", 30_000)) {
            log.info("Fail to get sync app lock!Skip sync.");
            return;
        }
        log.info("Get job-execute-sync-app lock successfully!");
        syncAppAndRefreshCacheTask.execute();
    }


}
