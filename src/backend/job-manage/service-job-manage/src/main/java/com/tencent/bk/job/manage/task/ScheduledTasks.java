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

package com.tencent.bk.job.manage.task;

import com.tencent.bk.job.manage.common.client.PAASClientFactory;
import com.tencent.bk.job.manage.manager.app.ApplicationCache;
import com.tencent.bk.job.manage.service.SyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @Description 增加调度需要注意到ScheduleConfig中更新线程池配置
 * @Date 2020/1/3
 * @Version 1.0
 */
@Slf4j
@Component
@EnableScheduling
public class ScheduledTasks {

    private final EsbUserInfoUpdateTask esbUserInfoUpdateTask;
    private final SyncService syncService;
    private final UserUploadFileCleanTask userUploadFileCleanTask;
    private final ApplicationCache applicationCache;

    @Autowired
    public ScheduledTasks(
        EsbUserInfoUpdateTask esbUserInfoUpdateTask,
        SyncService syncService,
        UserUploadFileCleanTask userUploadFileCleanTask,
        ApplicationCache applicationCache) {
        this.esbUserInfoUpdateTask = esbUserInfoUpdateTask;
        this.syncService = syncService;
        this.userUploadFileCleanTask = userUploadFileCleanTask;
        this.applicationCache = applicationCache;
    }

    /**
     * 每间隔1h更新一次人员数据
     */
    @Scheduled(initialDelay = 2 * 1000, fixedDelay = 60 * 60 * 1000)
    public void updateEsbUserInfo() {
        log.info("updateEsbUserInfo");
        try {
            esbUserInfoUpdateTask.execute();
        } catch (Exception e) {
            log.error("updateEsbUserInfo fail", e);
        }
    }

    /**
     * 业务同步：1min/次
     */
    @Scheduled(cron = "0 * * * * ?")
    public void appSyncTask() {
        log.info(Thread.currentThread().getId() + ":appSyncTask start");
        try {
            syncService.syncApp();
        } catch (Exception e) {
            log.error("testAppSyncTask fail", e);
        }
    }

    /**
     * 业务缓存刷新：1min/次
     */
    @Scheduled(cron = "0 * * * * ?")
    public void refreshAppCache() {
        log.info(Thread.currentThread().getId() + ":refreshAppCache start");
        try {
            applicationCache.refreshCache();
        } catch (Exception e) {
            log.error("refreshAppCache fail", e);
        }
    }

    /**
     * 主机同步：5min/次
     */
    @Scheduled(cron = "0 0/5 * * * ?")
    public void hostSyncTask() {
        log.info(Thread.currentThread().getId() + ":hostSyncTask start");
        try {
            syncService.syncHost();
        } catch (Exception e) {
            log.error("hostSyncTask fail", e);
        }
    }

    /**
     * Agent状态同步：3min/次
     */
    @Scheduled(cron = "0 0/3 * * * ?")
    public void agentStatusSyncTask() {
        log.info(Thread.currentThread().getId() + ":agentStatusSyncTask start");
        try {
            syncService.syncAgentStatus();
        } catch (Exception e) {
            log.error("agentStatusSyncTask fail", e);
        }
    }

    @Scheduled(cron = "0 4 * * * ?")
    public void cleanUserUploadFileTask() {
        log.info("Clean user upload file task begin");
        try {
            userUploadFileCleanTask.execute();
        } catch (Exception e) {
            log.error("Clean user upload file failed!", e);
        }
        log.info("Clean user upload file task finished");
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void resetTodayStatistics() {
        log.info("resetTodayStatistics begin");
        try {
            PAASClientFactory.resetTodayStatistics();
        } catch (Exception e) {
            log.error("resetTodayStatistics failed!", e);
        }
        log.info("resetTodayStatistics finished");
    }
}
