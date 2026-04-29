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

package com.tencent.bk.job.crontab.service.impl;

import com.tencent.bk.job.common.annotation.TenantMigrate;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.crontab.auth.CronAuthService;
import com.tencent.bk.job.crontab.dao.CronJobDAO;
import com.tencent.bk.job.crontab.exception.TaskExecuteAuthFailedException;
import com.tencent.bk.job.crontab.listener.event.CrontabEvent;
import com.tencent.bk.job.crontab.model.dto.CronJobInfoDTO;
import com.tencent.bk.job.crontab.model.dto.CronJobVariableDTO;
import com.tencent.bk.job.crontab.mq.CrontabMQEventDispatcher;
import com.tencent.bk.job.crontab.service.CronJobMigrationService;
import com.tencent.bk.job.crontab.service.CronJobService;
import com.tencent.bk.job.crontab.service.ExecuteTaskService;
import com.tencent.bk.job.execute.model.inner.ServiceTaskVariable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 定时任务迁移专用 Service 实现
 */
@TenantMigrate
@Slf4j
@Service
public class CronJobMigrationServiceImpl implements CronJobMigrationService {

    private final CronJobService cronJobService;
    private final CronJobDAO cronJobDAO;
    private final CronAuthService cronAuthService;
    private final ExecuteTaskService executeTaskService;
    private final CrontabMQEventDispatcher crontabMQEventDispatcher;

    @Autowired
    public CronJobMigrationServiceImpl(CronJobService cronJobService,
                                       CronJobDAO cronJobDAO,
                                       CronAuthService cronAuthService,
                                       ExecuteTaskService executeTaskService,
                                       CrontabMQEventDispatcher crontabMQEventDispatcher) {
        this.cronJobService = cronJobService;
        this.cronJobDAO = cronJobDAO;
        this.cronAuthService = cronAuthService;
        this.executeTaskService = executeTaskService;
        this.crontabMQEventDispatcher = crontabMQEventDispatcher;
    }

    @Override
    public Boolean changeCronJobEnableStatusForMigration(User user, Long appId, Long cronJobId, Boolean enable) {
        cronAuthService.authManageCron(user,
            new AppResourceScope(appId), cronJobId, null).denyIfNoPermission();

        CronJobInfoDTO originCronJobInfo = cronJobService.getCronJobInfoById(appId, cronJobId);
        if (originCronJobInfo == null) {
            throw new NotFoundException(ErrorCode.CRON_JOB_NOT_EXIST);
        }

        // 不修改 lastModifyUser 和 lastModifyTime，保留原始值，只修改定时状态
        originCronJobInfo.setEnable(enable);
        if (enable) {
            try {
                List<ServiceTaskVariable> taskVariables = null;
                if (CollectionUtils.isNotEmpty(originCronJobInfo.getVariableValue())) {
                    taskVariables =
                        originCronJobInfo.getVariableValue().stream()
                            .map(CronJobVariableDTO::toServiceTaskVariable).collect(Collectors.toList());
                }
                executeTaskService.authExecuteTask(appId, originCronJobInfo.getTaskPlanId(),
                    cronJobId, originCronJobInfo.getName(), taskVariables, user.getUsername());
                if (cronJobDAO.updateCronJobById(originCronJobInfo)) {
                    return informAllToAddJobToQuartz(appId, cronJobId);
                } else {
                    return false;
                }
            } catch (TaskExecuteAuthFailedException e) {
                log.error("Error while pre auth cron execute!", e);
                throw e;
            }
        } else {
            if (cronJobDAO.updateCronJobById(originCronJobInfo)) {
                return informAllToDeleteJobFromQuartz(appId, cronJobId);
            } else {
                return false;
            }
        }
    }

    private boolean informAllToAddJobToQuartz(long appId, long cronJobId) throws ServiceException {
        try {
            crontabMQEventDispatcher.broadCastCrontabEvent(CrontabEvent.addCron(appId, cronJobId));
            return true;
        } catch (Exception e) {
            log.error("Fail to broadCast addCronEvent", e);
            return false;
        }
    }

    private boolean informAllToDeleteJobFromQuartz(long appId, long cronJobId) {
        try {
            crontabMQEventDispatcher.broadCastCrontabEvent(CrontabEvent.deleteCron(appId, cronJobId));
            return true;
        } catch (Exception e) {
            log.error("Fail to broadCast deleteCronEvent", e);
            return false;
        }
    }
}
