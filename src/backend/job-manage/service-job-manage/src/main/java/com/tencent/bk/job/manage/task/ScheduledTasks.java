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

package com.tencent.bk.job.manage.task;

import com.tencent.bk.job.manage.background.ha.BackGroundTaskBalancer;
import com.tencent.bk.job.manage.background.ha.BackGroundTaskDaemon;
import com.tencent.bk.job.manage.background.sync.AgentStatusSyncService;
import com.tencent.bk.job.manage.background.sync.AllTenantHostSyncService;
import com.tencent.bk.job.manage.background.sync.AppSyncService;
import com.tencent.bk.job.manage.manager.app.ApplicationCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component("jobManageScheduledTasks")
@EnableScheduling
public class ScheduledTasks {

    private final AppSyncService appSyncService;
    private final AllTenantHostSyncService allTenantHostSyncService;
    private final AgentStatusSyncService agentStatusSyncService;
    private final UserUploadFileCleanTask userUploadFileCleanTask;
    private final ApplicationCache applicationCache;
    private final BackGroundTaskBalancer backGroundTaskBalancer;
    private final BackGroundTaskDaemon backGroundTaskDaemon;

    @Autowired
    public ScheduledTasks(
        AppSyncService appSyncService,
        AllTenantHostSyncService allTenantHostSyncService,
        AgentStatusSyncService agentStatusSyncService,
        UserUploadFileCleanTask userUploadFileCleanTask,
        ApplicationCache applicationCache,
        BackGroundTaskBalancer backGroundTaskBalancer,
        BackGroundTaskDaemon backGroundTaskDaemon) {
        this.appSyncService = appSyncService;
        this.allTenantHostSyncService = allTenantHostSyncService;
        this.agentStatusSyncService = agentStatusSyncService;
        this.userUploadFileCleanTask = userUploadFileCleanTask;
        this.applicationCache = applicationCache;
        this.backGroundTaskBalancer = backGroundTaskBalancer;
        this.backGroundTaskDaemon = backGroundTaskDaemon;
    }

    /**
     * 业务同步：1min/次
     */
    @Scheduled(cron = "0 * * * * ?")
    public void appSyncTask() {
        log.info(Thread.currentThread().getId() + ":appSyncTask start");
        try {
            appSyncService.syncApp();
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
            allTenantHostSyncService.syncHost();
        } catch (Exception e) {
            log.error("hostSyncTask fail", e);
        }
    }

    /**
     * Agent状态同步：1min/次
     */
    @Scheduled(cron = "0 * * * * ?")
    public void agentStatusSyncTask() {
        log.info(Thread.currentThread().getId() + ":agentStatusSyncTask start");
        try {
            agentStatusSyncService.syncAgentStatus();
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

    /**
     * 每分钟均衡一次分布在多个实例上的后台任务
     */
    @Scheduled(cron = "0 * * * * ?")
    public void balanceBackGroundTask() {
        log.info("balanceBackGroundTask begin");
        try {
            log.info("balanceBackGroundTask finished:{}", backGroundTaskBalancer.balance());
        } catch (Exception e) {
            log.error("balanceBackGroundTask failed!", e);
        }
    }

    /**
     * 每分钟检查并恢复一次异常终止的后台任务
     */
    @Scheduled(cron = "0 * * * * ?")
    public void runBackGroundTaskDaemon() {
        log.info("balanceBackGroundTask begin");
        try {
            backGroundTaskDaemon.checkAndResumeTaskForAllTenant(false);
        } catch (Exception e) {
            log.error("balanceBackGroundTask failed!", e);
        }
    }
}
