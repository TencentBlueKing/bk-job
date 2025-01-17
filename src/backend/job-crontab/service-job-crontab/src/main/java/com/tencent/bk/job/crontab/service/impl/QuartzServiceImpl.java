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

package com.tencent.bk.job.crontab.service.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.crontab.constant.CronConstants;
import com.tencent.bk.job.crontab.model.dto.CronJobInfoDTO;
import com.tencent.bk.job.crontab.service.QuartzService;
import com.tencent.bk.job.crontab.timer.AbstractQuartzTaskHandler;
import com.tencent.bk.job.crontab.timer.QuartzJob;
import com.tencent.bk.job.crontab.timer.QuartzJobBuilder;
import com.tencent.bk.job.crontab.timer.QuartzTrigger;
import com.tencent.bk.job.crontab.timer.QuartzTriggerBuilder;
import com.tencent.bk.job.crontab.timer.executor.NotifyJobExecutor;
import com.tencent.bk.job.crontab.timer.executor.SimpleJobExecutor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.quartz.CronTrigger;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.Instant;

@Slf4j
@Service
public class QuartzServiceImpl implements QuartzService {

    private final AbstractQuartzTaskHandler quartzTaskHandler;

    @Autowired
    public QuartzServiceImpl(AbstractQuartzTaskHandler quartzTaskHandler) {
        this.quartzTaskHandler = quartzTaskHandler;
    }

    @Override
    public void tryToAddJobToQuartz(CronJobInfoDTO cronJobInfo) {
        try {
            addJobToQuartz(cronJobInfo);
        } catch (ServiceException e) {
            deleteJobFromQuartz(cronJobInfo.getAppId(), cronJobInfo.getId());
            throw e;
        } catch (Exception e) {
            deleteJobFromQuartz(cronJobInfo.getAppId(), cronJobInfo.getId());
            throw new InternalException(e, ErrorCode.INTERNAL_ERROR);
        }
    }

    /**
     * 从Quartz引擎删除定时任务（含相关的通知任务）
     *
     * @param appId     Job业务ID
     * @param cronJobId 定时任务ID
     * @return 是否删除成功
     */
    @Override
    public boolean deleteJobFromQuartz(long appId, long cronJobId) {
        if (appId <= 0 || cronJobId <= 0) {
            return false;
        }
        String jobName = JobCronNameUtil.getJobName(cronJobId);
        String jobGroup = JobCronNameUtil.getJobGroup(appId);
        String notifyJobName = JobCronNameUtil.getNotifyJobName(cronJobId);
        try {
            quartzTaskHandler.deleteJob(JobKey.jobKey(jobName, jobGroup));
            quartzTaskHandler.deleteJob(JobKey.jobKey(notifyJobName, jobGroup));
            return true;
        } catch (SchedulerException e) {
            log.error("Error while delete job!", e);
        }
        return false;
    }

    /**
     * 将定时任务添加至Quartz引擎（含相关的前置通知任务）
     *
     * @param cronJobInfo 定时任务信息
     * @throws SchedulerException Quartz调度异常
     */
    private void addJobToQuartz(CronJobInfoDTO cronJobInfo) throws SchedulerException {
        Long appId = cronJobInfo.getAppId();
        Long cronJobId = cronJobInfo.getId();
        String jobName = JobCronNameUtil.getJobName(cronJobId);
        String jobGroup = JobCronNameUtil.getJobGroup(appId);
        QuartzTrigger trigger = buildTrigger(cronJobInfo);

        QuartzJob job = QuartzJobBuilder.newJob()
            .withIdentity(jobName, jobGroup)
            .forJob(SimpleJobExecutor.class)
            .usingJobData(CronConstants.JOB_DATA_KEY_APP_ID_STR, String.valueOf(appId))
            .usingJobData(CronConstants.JOB_DATA_KEY_CRON_JOB_ID_STR, String.valueOf(cronJobId))
            .withTrigger(trigger)
            .build();

        quartzTaskHandler.deleteJob(JobKey.jobKey(jobName, jobGroup));
        quartzTaskHandler.addJob(job);

        addNotifyJobIfNeed(cronJobInfo);
    }

    /**
     * 构建定时任务Quartz触发器
     *
     * @param cronJobInfo 定时任务信息
     * @return 定时任务Quartz触发器
     */
    private QuartzTrigger buildTrigger(CronJobInfoDTO cronJobInfo) {
        QuartzTrigger trigger;
        String jobName = JobCronNameUtil.getJobName(cronJobInfo.getId());
        String jobGroup = JobCronNameUtil.getJobGroup(cronJobInfo.getAppId());
        if (StringUtils.isNotBlank(cronJobInfo.getCronExpression())) {
            // 根据cron表达式执行的定时任务
            QuartzTriggerBuilder cronTriggerBuilder = QuartzTriggerBuilder.newTrigger()
                .ofType(QuartzTrigger.TriggerType.CRON)
                .withIdentity(jobName, jobGroup)
                .withCronExpression(cronJobInfo.getCronExpression())
                .withMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
            if (cronJobInfo.getEndTime() > 0) {
                if (cronJobInfo.getEndTime() < DateUtils.currentTimeSeconds()) {
                    throw new FailedPreconditionException(ErrorCode.END_TIME_OR_NOTIFY_TIME_ALREADY_PASSED);
                } else {
                    cronTriggerBuilder = cronTriggerBuilder.
                        endAt(Date.from(Instant.ofEpochSecond(cronJobInfo.getEndTime())));
                }
            }
            trigger = cronTriggerBuilder.build();
        } else if (cronJobInfo.getExecuteTime() > DateUtils.currentTimeSeconds()) {
            // 只执行一次的定时任务
            trigger = QuartzTriggerBuilder.newTrigger().ofType(QuartzTrigger.TriggerType.SIMPLE)
                .withIdentity(jobName, jobGroup)
                .startAt(Date.from(Instant.ofEpochSecond(cronJobInfo.getExecuteTime()))).withRepeatCount(0)
                .withIntervalInHours(1)
                .withMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT)
                .build();
        } else {
            // 只执行一次但是执行时间已经过期的定时任务
            String message = MessageFormatter.format(
                "Cron job executionTime({}) already passed",
                TimeUtil.formatTime(cronJobInfo.getExecuteTime())
            ).getMessage();
            throw new FailedPreconditionException(message, ErrorCode.CRON_JOB_TIME_PASSED);
        }
        return trigger;
    }

    /**
     * 按需添加通知任务至Quartz引擎
     *
     * @param cronJobInfo 定时任务信息
     * @throws SchedulerException Quartz调度异常
     */
    private void addNotifyJobIfNeed(CronJobInfoDTO cronJobInfo) throws SchedulerException {
        Long appId = cronJobInfo.getAppId();
        Long cronJobId = cronJobInfo.getId();
        String notifyJobName = JobCronNameUtil.getNotifyJobName(cronJobId);
        String jobGroup = JobCronNameUtil.getJobGroup(appId);
        if (cronJobInfo.getNotifyOffset() > 0) {
            long notifyTime = 0L;
            if (StringUtils.isNotBlank(cronJobInfo.getCronExpression())) {
                if (cronJobInfo.getEndTime() > 0) {
                    notifyTime = cronJobInfo.getEndTime() - cronJobInfo.getNotifyOffset();
                }
            } else {
                notifyTime = cronJobInfo.getExecuteTime() - cronJobInfo.getNotifyOffset();
            }
            if (notifyTime < DateUtils.currentTimeSeconds()) {
                throw new FailedPreconditionException(ErrorCode.END_TIME_OR_NOTIFY_TIME_ALREADY_PASSED);
            }
            QuartzTrigger notifyTrigger = QuartzTriggerBuilder.newTrigger()
                .ofType(QuartzTrigger.TriggerType.SIMPLE)
                .withIdentity(notifyJobName, jobGroup)
                .startAt(Date.from(Instant.ofEpochSecond(notifyTime))).withRepeatCount(0).withIntervalInHours(1)
                .withMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT)
                .build();

            QuartzJob notifyJob = QuartzJobBuilder.newJob()
                .withIdentity(notifyJobName, jobGroup)
                .forJob(NotifyJobExecutor.class)
                .usingJobData(CronConstants.JOB_DATA_KEY_APP_ID_STR, String.valueOf(appId))
                .usingJobData(CronConstants.JOB_DATA_KEY_CRON_JOB_ID_STR, String.valueOf(cronJobId))
                .withTrigger(notifyTrigger)
                .build();

            quartzTaskHandler.deleteJob(JobKey.jobKey(notifyJobName, jobGroup));
            quartzTaskHandler.addJob(notifyJob);
        } else {
            quartzTaskHandler.deleteJob(JobKey.jobKey(notifyJobName, jobGroup));
        }
    }

}
