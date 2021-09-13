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

package com.tencent.bk.job.crontab.timer.executor;

import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.crontab.constant.ExecuteStatusEnum;
import com.tencent.bk.job.crontab.constant.LastExecuteStatusEnum;
import com.tencent.bk.job.crontab.model.dto.CronJobHistoryDTO;
import com.tencent.bk.job.crontab.model.dto.CronJobInfoDTO;
import com.tencent.bk.job.crontab.model.dto.CronJobVariableDTO;
import com.tencent.bk.job.crontab.service.CronJobHistoryService;
import com.tencent.bk.job.crontab.service.CronJobService;
import com.tencent.bk.job.crontab.service.ExecuteTaskService;
import com.tencent.bk.job.crontab.service.NotifyService;
import com.tencent.bk.job.crontab.timer.AbstractQuartzJobBean;
import com.tencent.bk.job.execute.model.inner.ServiceTaskExecuteResult;
import com.tencent.bk.job.execute.model.inner.ServiceTaskVariable;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 普通定时任务执行器
 *
 * @since 16/2/2020 15:38
 */
@Slf4j
@Setter
public class SimpleJobExecutor extends AbstractQuartzJobBean {

    @Autowired
    CronJobService cronJobService;

    @Autowired
    CronJobHistoryService cronJobHistoryService;

    @Autowired
    ExecuteTaskService executeTaskService;

    @Autowired
    NotifyService notifyService;

    @Value("${notification-policy.failed.start}")
    private Integer startFailedNotification;

    /**
     * 业务 ID 字符串
     */
    private String appIdStr;

    /**
     * 定时任务 ID 字符串
     */
    private String cronJobIdStr;

    @Override
    public String name() {
        return "SimpleCronJob";
    }

    @Override
    protected void executeInternalInternal(JobExecutionContext context) throws JobExecutionException {
        // Parse basic info
        if (log.isDebugEnabled()) {
            log.debug("Execute task|{}|{}", appIdStr, cronJobIdStr);
        }
        long appId = Long.parseLong(appIdStr);
        long cronJobId = Long.parseLong(cronJobIdStr);
        long scheduledFireTime = getScheduledFireTime(context).toEpochMilli();

        CronJobHistoryDTO cronJobHistory =
            cronJobHistoryService.getHistoryByIdAndTime(appId, cronJobId, scheduledFireTime);
        if (cronJobHistory != null) {
            log.warn("Job already running!|{}", cronJobHistory);
            return;
        }

        long historyId = cronJobHistoryService.insertHistory(appId, cronJobId, scheduledFireTime);
        if (log.isDebugEnabled()) {
            log.debug("Insert history finished!|{}", historyId);
        }

        CronJobInfoDTO cronJobInfo = cronJobService.getCronJobInfoById(appId, cronJobId);
        if (log.isDebugEnabled()) {
            log.debug("Get cronjob info return|{}", cronJobInfo);
        }

        CronJobInfoDTO cronJobErrorInfo = cronJobService.getCronJobErrorInfoById(appId, cronJobId);
        if (log.isDebugEnabled()) {
            log.debug("Get cronjob Error info return|{}", cronJobErrorInfo);
        }
        Integer lastExecuteStatus = cronJobErrorInfo.getLastExecuteStatus();
        Long lastExecuteErrorCode = cronJobErrorInfo.getLastExecuteErrorCode();
        Integer lastExecuteErrorCount = cronJobErrorInfo.getLastExecuteErrorCount();

        List<CronJobVariableDTO> variables = cronJobInfo.getVariableValue();
        List<ServiceTaskVariable> taskVariables = null;
        if (CollectionUtils.isNotEmpty(variables)) {
            taskVariables =
                variables.parallelStream().map(CronJobVariableDTO::toServiceTaskVariable).collect(Collectors.toList());
        }

        boolean executeFailed = false;
        Integer errorCode = null;
        String errorMessage = null;
        cronJobHistoryService.fillExecutor(historyId, cronJobInfo.getLastModifyUser());
        ServiceResponse<ServiceTaskExecuteResult> executeResult = executeTaskService.executeTask(appId,
            cronJobInfo.getTaskPlanId(),
            cronJobInfo.getId(), cronJobInfo.getName(), taskVariables, cronJobInfo.getLastModifyUser());
        if (log.isDebugEnabled()) {
            log.debug("Execute result|{}", executeResult);
        }
        if (executeResult != null && executeResult.getData() != null
            && executeResult.getData().getTaskInstanceId() > 0) {
            if (log.isDebugEnabled()) {
                log.debug("Execute success! Task instance id {}", executeResult.getData().getTaskInstanceId());
            }
            cronJobHistoryService.updateStatusByIdAndTime(appId, cronJobId, scheduledFireTime,
                ExecuteStatusEnum.RUNNING);
            cronJobErrorInfo.setLastExecuteStatus(LastExecuteStatusEnum.SUCCESS.getValue());
            cronJobErrorInfo.setLastExecuteErrorCode(null);
            cronJobErrorInfo.setLastExecuteErrorCount(0);
        } else {
            log.error("Execute task failed!|{}|{}|{}|{}", appId, cronJobId, scheduledFireTime, executeResult);
            cronJobHistoryService.updateStatusByIdAndTime(appId, cronJobId, scheduledFireTime, ExecuteStatusEnum.FAIL);
            cronJobErrorInfo.setLastExecuteStatus(LastExecuteStatusEnum.FAIL.getValue());
            executeFailed = true;
            if (executeResult != null) {
                errorCode = executeResult.getCode();
                errorMessage = executeResult.getErrorMsg();
                if (errorCode != null) {
                    cronJobHistoryService.fillErrorInfo(historyId, errorCode.longValue(), errorMessage);
                    cronJobErrorInfo.setLastExecuteErrorCode(errorCode.longValue());
                    if (lastExecuteErrorCode == null || errorCode.longValue() == lastExecuteErrorCode) {
                        cronJobErrorInfo.setLastExecuteErrorCount(lastExecuteErrorCount + 1);
                    } else {
                        cronJobErrorInfo.setLastExecuteErrorCount(1);
                    }
                }
            }
        }

        if (cronJobService.updateCronJobErrorById(cronJobErrorInfo)) {
            if (log.isDebugEnabled()) {
                log.debug("Update success! New cronjob simple info|{}", cronJobErrorInfo);
            }
        }

        if (context.getNextFireTime() == null) {
            cronJobService.disableExpiredCronJob(appId, cronJobId, cronJobInfo.getLastModifyUser(),
                cronJobInfo.getLastModifyTime());
        }

        if (executeFailed && isNotify(cronJobErrorInfo, lastExecuteStatus)) {
            notifyService.sendCronJobFailedNotification(errorCode, errorMessage, cronJobInfo);
            if (log.isDebugEnabled()) {
                log.debug("Send cronjob failed notification, execute error count is {}", cronJobErrorInfo.getLastExecuteErrorCount());
            }
        }
    }


    private boolean isNotify(CronJobInfoDTO cronJobErrorInfo, Integer lastExecuteStatus) {

        Integer executeStatus = cronJobErrorInfo.getLastExecuteStatus();
        Integer executeErrorCount = cronJobErrorInfo.getLastExecuteErrorCount();

        if (log.isDebugEnabled()) {
            log.debug("Start failed notification strategy is {}", startFailedNotification);
        }

        if (startFailedNotification == 0) {
            if (executeErrorCount == 1 || !lastExecuteStatus.equals(executeStatus)) {
                return true;
            }
        } else if (startFailedNotification == 1) {
            return true;
        } else if (executeErrorCount % startFailedNotification == 1 || !lastExecuteStatus.equals(executeStatus)) {
            return true;
        }
        return false;
    }
}
