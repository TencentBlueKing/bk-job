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

package com.tencent.bk.job.crontab.timer;

import com.tencent.bk.job.crontab.model.dto.QuartzJobInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.quartz.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 定时任务控制器
 */
@Slf4j
public abstract class AbstractQuartzTaskHandler {

    protected Scheduler scheduler;

    /**
     * 动态添加任务
     * <p>
     * 将声明的 QuartzJob 添加到 Scheduler 中
     *
     * @param quartzJob 定时任务信息
     * @throws SchedulerException 如果声明的定时任务不能被添加到 Scheduler 中，或者 Scheduler 内部异常
     * @see QuartzJob 定时任务信息
     */
    public abstract void addJob(QuartzJob quartzJob) throws SchedulerException;

    /**
     * 动态添加任务
     * <p>
     * 将声明的 QuartzJob 添加到 Scheduler 中
     *
     * @param quartzJobs 定时任务信息列表
     * @throws SchedulerException 如果声明的定时任务不能被添加到 Scheduler 中，或者 Scheduler 内部异常
     * @see QuartzJob 定时任务信息
     * @see #addJob(QuartzJob)
     */
    public abstract void addJob(QuartzJob... quartzJobs) throws SchedulerException;

    /**
     * 删除任务
     * <p>
     * 将声明的 QuartzJob 从 Scheduler 中删除
     *
     * @param jobKey 定时任务Key
     * @throws SchedulerException 如果定时任务不能被删除，或者 Scheduler 内部异常
     * @see QuartzJob
     */
    public abstract void deleteJob(JobKey jobKey) throws SchedulerException;

    /**
     * 删除任务
     * <p>
     * 将声明的 QuartzJob 从 Scheduler 中删除
     *
     * @param jobKeys 定时任务Key列表
     * @throws SchedulerException 如果定时任务不能被删除，或者 Scheduler 内部异常
     * @see QuartzJob
     */
    public abstract void deleteJob(List<JobKey> jobKeys) throws SchedulerException;

    /**
     * 暂停任务Trigger
     * <p>
     * 将 Scheduler 中的所有任务暂停
     *
     * @throws SchedulerException 如果定时任务不能被暂停，或者 Scheduler 内部异常
     * @see QuartzJob 定时任务信息
     */
    public abstract void pauseAll() throws SchedulerException;

    /**
     * Determine whether a Job with the given identifier already exists within the scheduler.
     *
     * @param jobKey the identifier to check for
     * @return true if a Job exists with the given identifier
     * @throws SchedulerException 内部错误
     */
    public boolean checkExists(JobKey jobKey) throws SchedulerException {
        return this.scheduler.checkExists(jobKey);
    }

    /**
     * 创建JobDetail
     *
     * @param quartzJob {@link QuartzJob}
     * @return {@link JobDetail}
     * @throws SchedulerException QuartzJob 属性错误、 Scheduler 内部异常
     */
    protected JobDetail createJobDetail(QuartzJob quartzJob) throws SchedulerException {
        JobBuilder jobBuilder = JobBuilder.newJob(quartzJob.getJobClass()).withIdentity(quartzJob.getKey())
            .withDescription(quartzJob.getDescription()).storeDurably();

        if (quartzJob.getJobData() != null) {
            jobBuilder.usingJobData(new JobDataMap(quartzJob.getJobData()));
        }

        return jobBuilder.build();
    }

    /**
     * 创建Trigger
     *
     * @param quartzTrigger {@link QuartzTrigger}
     * @return {@link Trigger}
     * @throws SchedulerException QuartzTrigger 属性错误、 Scheduler 内部异常
     */
    protected Trigger createTrigger(QuartzTrigger quartzTrigger) throws SchedulerException {
        TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger().withIdentity(quartzTrigger.getKey())
            .withSchedule(initCronScheduleBuilder(quartzTrigger));

        if (quartzTrigger.getJobKey() != null) {
            triggerBuilder.forJob(quartzTrigger.getJobKey());
        }

        if (quartzTrigger.getJobData() != null) {
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.putAll(quartzTrigger.getJobData());
            triggerBuilder.usingJobData(jobDataMap);
        }

        if (StringUtils.isNotEmpty(quartzTrigger.getCalendarName())) {
            Calendar calendarInScheduler = this.scheduler.getCalendar(quartzTrigger.getCalendarName());
            if (calendarInScheduler == null) {
                log.error("Calendar {} not found in Scheduler!", quartzTrigger.getCalendarName());
                throw new SchedulerException("Calendar " + quartzTrigger.getCalendarName() + "not found in Scheduler!");
            }

            triggerBuilder.modifiedByCalendar(quartzTrigger.getCalendarName());
        }

        if (quartzTrigger.getStartAt() != null) {
            triggerBuilder.startAt(quartzTrigger.getStartAt());
        }

        if (quartzTrigger.isStartNow()) {
            triggerBuilder.startNow();
        }

        if (quartzTrigger.getEndAt() != null) {
            triggerBuilder.endAt(quartzTrigger.getEndAt());
        }

        return triggerBuilder.build();
    }

    /**
     * 创建Trigger
     *
     * @param quartzJob {@link QuartzJob}
     * @return {@link Trigger}集合
     * @throws SchedulerException QuartzTrigger 属性错误、 Scheduler 内部异常
     */
    protected Set<Trigger> createTriggers(QuartzJob quartzJob) throws SchedulerException {
        if (quartzJob.getTriggers() == null) {
            return null;
        }

        Set<Trigger> triggers = new HashSet<>();

        for (QuartzTrigger quartzTrigger : quartzJob.getTriggers()) {
            Trigger trigger = createTrigger(quartzTrigger);

            triggers.add(trigger);
        }

        return triggers;
    }

    /**
     * 根据定时任务信息组织CronScheduleBuilder
     *
     * @param quartzTrigger 定时任务信息
     * @return CronScheduleBuilder 基于Cron表达式的 ScheduleBuilder
     * @see CronScheduleBuilder
     * @see QuartzJob
     * @see CronScheduleBuilder
     */
    protected ScheduleBuilder initCronScheduleBuilder(QuartzTrigger quartzTrigger) {
        ScheduleBuilder scheduleBuilder;
        if (QuartzTrigger.TriggerType.SIMPLE.equals(quartzTrigger.getType())) {
            SimpleScheduleBuilder simpleScheduleBuilder =
                SimpleScheduleBuilder.simpleSchedule().withIntervalInMilliseconds(quartzTrigger.getRepeatInterval());

            if (quartzTrigger.getRepeatCount() == SimpleTrigger.REPEAT_INDEFINITELY) {
                simpleScheduleBuilder.repeatForever();
            } else {
                simpleScheduleBuilder.withRepeatCount(quartzTrigger.getRepeatCount());
            }

            if (quartzTrigger.getMisfireInstruction() == SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW) {
                simpleScheduleBuilder.withMisfireHandlingInstructionFireNow();
            } else if (quartzTrigger
                .getMisfireInstruction() == SimpleTrigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY) {
                simpleScheduleBuilder.withMisfireHandlingInstructionIgnoreMisfires();
            } else if (quartzTrigger
                .getMisfireInstruction() == SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT) {
                simpleScheduleBuilder.withMisfireHandlingInstructionNextWithExistingCount();
            } else if (quartzTrigger
                .getMisfireInstruction() == SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT) {
                simpleScheduleBuilder.withMisfireHandlingInstructionNextWithRemainingCount();
            } else if (quartzTrigger
                .getMisfireInstruction()
                == SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT) {
                simpleScheduleBuilder.withMisfireHandlingInstructionNowWithExistingCount();
            } else if (quartzTrigger
                .getMisfireInstruction()
                == SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT) {
                simpleScheduleBuilder.withMisfireHandlingInstructionNowWithRemainingCount();
            }

            scheduleBuilder = simpleScheduleBuilder;
        } else {
            CronScheduleBuilder cronScheduleBuilder =
                CronScheduleBuilder.cronSchedule(quartzTrigger.getCronExpression());

            // 以当前时间为触发频率立刻触发一次执行，然后按照Cron频率依次执行
            if (quartzTrigger.getMisfireInstruction() == CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW) {
                cronScheduleBuilder.withMisfireHandlingInstructionFireAndProceed();
            } else if (quartzTrigger.getMisfireInstruction()
                == CronTrigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY) {
                // 以错过的第一个频率时间立刻开始执行，重做错过的所有频率周期后，
                // 当下一次触发频率发生时间大于当前时间后，再按照正常的Cron频率依次执行
                cronScheduleBuilder.withMisfireHandlingInstructionIgnoreMisfires();
            } else if (quartzTrigger.getMisfireInstruction() == CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING) {
                // 不触发立即执行，等待下次Cron触发频率到达时刻开始按照Cron频率依次执行
                cronScheduleBuilder.withMisfireHandlingInstructionDoNothing();
            }

            scheduleBuilder = cronScheduleBuilder;
        }

        return scheduleBuilder;
    }

    public QuartzJobInfoDTO getJobInfo(String systemId, String jobKey) throws SchedulerException {
        Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey(jobKey, systemId));
        if (trigger instanceof CronTrigger) {
            QuartzJobInfoDTO innerCronJobInfo = new QuartzJobInfoDTO();
            innerCronJobInfo.setName(jobKey);
            innerCronJobInfo.setGroup(systemId);
            innerCronJobInfo.setDescription(trigger.getDescription());
            innerCronJobInfo.setCronExpression(((CronTrigger) trigger).getCronExpression());
            int rawOffset = ((CronTrigger) trigger).getTimeZone().getRawOffset();
            innerCronJobInfo.setTimeZone(Long.valueOf(TimeUnit.MILLISECONDS.toHours(rawOffset)).intValue());
            if (trigger.getPreviousFireTime() != null) {
                innerCronJobInfo.setLastFiredTime(trigger.getPreviousFireTime().toInstant().getEpochSecond());
            }
            if (trigger.getNextFireTime() != null) {
                innerCronJobInfo.setNextFiredTime(trigger.getNextFireTime().toInstant().getEpochSecond());
            }
            innerCronJobInfo.setStartAt(trigger.getStartTime().toInstant().getEpochSecond());
            if (trigger.getEndTime() != null) {
                innerCronJobInfo.setEndAt(trigger.getEndTime().toInstant().getEpochSecond());
            }

            innerCronJobInfo.setJobDataMap(trigger.getJobDataMap());
            return innerCronJobInfo;
        }
        return null;
    }
}
