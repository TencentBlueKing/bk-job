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

package com.tencent.bk.job.manage.background.ha;

import com.tencent.bk.job.common.paas.model.OpenApiTenant;
import com.tencent.bk.job.common.paas.user.IUserApiClient;
import com.tencent.bk.job.manage.background.event.cmdb.CmdbEventManager;
import com.tencent.bk.job.manage.background.ha.mq.BackGroundTaskDispatcher;
import com.tencent.bk.job.manage.config.BackGroundTaskProperties;
import com.tencent.bk.job.manage.config.JobManageConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 后台任务守护机制，用于检查并恢复因异常（kill -9等）退出的后台任务
 */
@Slf4j
@Service
public class BackGroundTaskDaemon {

    private final CmdbEventManager cmdbEventManager;
    private final BackGroundTaskDispatcher backGroundTaskDispatcher;

    private final IUserApiClient userApiClient;
    private final JobManageConfig jobManageConfig;
    private final BackGroundTaskProperties backGroundTaskProperties;

    @Autowired
    public BackGroundTaskDaemon(CmdbEventManager cmdbEventManager,
                                BackGroundTaskDispatcher backGroundTaskDispatcher,
                                IUserApiClient userApiClient,
                                JobManageConfig jobManageConfig,
                                BackGroundTaskProperties backGroundTaskProperties) {
        this.cmdbEventManager = cmdbEventManager;
        this.backGroundTaskDispatcher = backGroundTaskDispatcher;
        this.userApiClient = userApiClient;
        this.jobManageConfig = jobManageConfig;
        this.backGroundTaskProperties = backGroundTaskProperties;
    }

    /**
     * 检查并恢复异常终止的后台任务
     *
     * @param onStartup 是否在启动时执行
     */
    public void checkAndResumeTaskForAllTenant(boolean onStartup) {
        if (!jobManageConfig.isEnableResourceWatch()) {
            log.info("resourceWatch not enabled, you can enable it in config file");
            return;
        }
        if (!backGroundTaskProperties.getDaemon().getEnabled()) {
            log.info("BackGroundTask daemon not enabled, you can enable it in config file");
            return;
        }
        List<OpenApiTenant> tenantList = userApiClient.listAllTenant();
        int resumedTaskCount = 0;
        // 遍历所有租户监听事件
        for (OpenApiTenant openApiTenant : tenantList) {
            String tenantId = openApiTenant.getId();
            resumedTaskCount += checkAndResumeTaskForTenant(tenantId);
        }
        if (resumedTaskCount > 0) {
            if (!onStartup) {
                log.warn("checkAndResumeTask finished, resumedTaskCount={}", resumedTaskCount);
            } else {
                log.info("{} backGroundTasks started", resumedTaskCount);
            }
        } else {
            if (onStartup) {
                log.info("All backGroundTasks are alive, no need to start");
            }
        }
    }

    /**
     * 检查并恢复租户下异常终止的后台任务
     *
     * @param tenantId 租户ID
     * @return 恢复的后台任务数量
     */
    public int checkAndResumeTaskForTenant(String tenantId) {
        int resumedTaskCount = 0;
        if (!cmdbEventManager.isWatchBizEventRunning(tenantId)) {
            backGroundTaskDispatcher.dispatch(new TaskEntity(BackGroundTaskCode.WATCH_BIZ, tenantId));
            log.info("Resumed watchBiz:{}", tenantId);
            resumedTaskCount++;
        }
        if (!cmdbEventManager.isWatchBizSetEventRunning(tenantId)) {
            backGroundTaskDispatcher.dispatch(new TaskEntity(BackGroundTaskCode.WATCH_BIZ_SET, tenantId));
            log.info("Resumed watchBizSet:{}", tenantId);
            resumedTaskCount++;
        }
        if (!cmdbEventManager.isWatchBizSetRelationEventRunning(tenantId)) {
            backGroundTaskDispatcher.dispatch(new TaskEntity(BackGroundTaskCode.WATCH_BIZ_SET_RELATION, tenantId));
            log.info("Resumed watchBizSetRelation:{}", tenantId);
            resumedTaskCount++;
        }
        if (!cmdbEventManager.isWatchHostEventRunning(tenantId)) {
            backGroundTaskDispatcher.dispatch(new TaskEntity(BackGroundTaskCode.WATCH_HOST, tenantId));
            log.info("Resumed watchHost:{}", tenantId);
            resumedTaskCount++;
        }
        if (!cmdbEventManager.isWatchHostRelationEventRunning(tenantId)) {
            backGroundTaskDispatcher.dispatch(new TaskEntity(BackGroundTaskCode.WATCH_HOST_RELATION, tenantId));
            log.info("Resumed watchHostRelation:{}", tenantId);
            resumedTaskCount++;
        }
        return resumedTaskCount;
    }
}
