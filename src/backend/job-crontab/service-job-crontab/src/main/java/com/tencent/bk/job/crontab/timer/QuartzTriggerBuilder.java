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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import org.quartz.DateBuilder;
import org.quartz.JobKey;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * QuartzTrigger to {@link Trigger}
 */
public class QuartzTriggerBuilder {

    private QuartzTrigger.TriggerType type = QuartzTrigger.TriggerType.SIMPLE;

    private JobKey jobKey;

    private TriggerKey key;

    private String description;

    private Map<String, Object> jobData = new HashMap<>();

    private String calendarName;

    /**
     * 重复次数，如永久重复，则设置该值为 {@link SimpleTrigger#REPEAT_INDEFINITELY}
     */
    private int repeatCount;

    /**
     * 重复时间间隔，单位：毫秒
     */
    private long interval;

    private String cronExpression;

    private int misfireInstruction = Trigger.MISFIRE_INSTRUCTION_SMART_POLICY;

    private Date startAt;

    private boolean startNow;

    private Date endAt;

    private QuartzTriggerBuilder() {

    }

    public static QuartzTriggerBuilder newTrigger() {
        return new QuartzTriggerBuilder();
    }

    public QuartzTrigger build() {
        QuartzTrigger quartzTrigger = new QuartzTrigger();

        quartzTrigger.setType(this.type);
        quartzTrigger.setJobKey(this.jobKey);

        if (key == null) {
            throw new InternalException(ErrorCode.INTERNAL_ERROR, "Trigger mast have key!");
        }

        quartzTrigger.setKey(this.key);

        quartzTrigger.setDescription(this.description);
        quartzTrigger.setCalendarName(this.calendarName);
        quartzTrigger.setStartAt(this.startAt);
        quartzTrigger.setStartNow(this.startNow);
        quartzTrigger.setEndAt(this.endAt);

        if (!this.jobData.isEmpty()) {
            quartzTrigger.setJobData(this.jobData);
        }

        quartzTrigger.setRepeatCount(this.repeatCount);
        quartzTrigger.setRepeatInterval(this.interval);
        quartzTrigger.setCronExpression(this.cronExpression);
        quartzTrigger.setMisfireInstruction(this.misfireInstruction);

        return quartzTrigger;
    }

    public QuartzTriggerBuilder ofType(QuartzTrigger.TriggerType type) {
        this.type = type;
        return this;
    }

    public QuartzTriggerBuilder forJob(String jobName, String jobGroup) {
        this.jobKey = new JobKey(jobName, jobGroup);
        return this;
    }

    public QuartzTriggerBuilder withIdentity(String name, String group) {
        this.key = new TriggerKey(name, group);
        return this;
    }

    public QuartzTriggerBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public QuartzTriggerBuilder withCalendarName(String calendarName) {
        this.calendarName = calendarName;
        return this;
    }

    public QuartzTriggerBuilder withRepeatCount(int repeatCount) {
        this.repeatCount = repeatCount;
        return this;
    }

    public QuartzTriggerBuilder repeatForever() {
        this.repeatCount = SimpleTrigger.REPEAT_INDEFINITELY;
        return this;
    }

    public QuartzTriggerBuilder withIntervalInSeconds(long intervalInSeconds) {
        this.interval = intervalInSeconds;
        return this;
    }

    public QuartzTriggerBuilder withIntervalInMinutes(int intervalInMinutes) {
        this.interval = intervalInMinutes * DateBuilder.MILLISECONDS_IN_MINUTE;
        return this;
    }

    public QuartzTriggerBuilder withIntervalInHours(int intervalInHours) {
        this.interval = intervalInHours * DateBuilder.MILLISECONDS_IN_HOUR;
        return this;
    }

    public QuartzTriggerBuilder withCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
        return this;
    }

    public QuartzTriggerBuilder withMisfireInstruction(int misfireInstruction) {
        this.misfireInstruction = misfireInstruction;
        return this;
    }

    public QuartzTriggerBuilder startAt(Date startAt) {
        this.startAt = startAt;
        return this;
    }

    public QuartzTriggerBuilder startNow(boolean startNow) {
        this.startNow = startNow;
        return this;
    }

    public QuartzTriggerBuilder endAt(Date endAt) {
        this.endAt = endAt;
        return this;
    }

    public QuartzTriggerBuilder setJobData(Map<String, Object> newJobData) {
        this.jobData.forEach(newJobData::put);
        this.jobData = newJobData;
        return this;
    }

    public QuartzTriggerBuilder usingJobData(String key, String value) {
        this.jobData.put(key, value);
        return this;
    }

}
