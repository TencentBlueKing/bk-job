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

import lombok.Data;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

@Data
public class QuartzTrigger implements Serializable {

    private TriggerType type = TriggerType.SIMPLE;
    private JobKey jobKey;
    private TriggerKey originalKey;
    private TriggerKey key;
    private String description;
    private String calendarName;
    private Date startAt;
    private boolean startNow;
    private Date endAt;
    private Map<String, Object> jobData;

    /**
     * 重复次数，如永久重复，则设置该值为
     *
     * @see org.quartz.SimpleTrigger#REPEAT_INDEFINITELY
     */
    private int repeatCount;

    /**
     * 重复时间间隔，单位：毫秒
     */
    private long repeatInterval;

    private String cronExpression;

    /**
     * 任务错过触发时间执行策略
     *
     * @see Trigger#MISFIRE_INSTRUCTION_SMART_POLICY
     * @see Trigger#MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY
     * @see org.quartz.SimpleTrigger#MISFIRE_INSTRUCTION_FIRE_NOW
     * @see org.quartz.SimpleTrigger#MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT
     * @see org.quartz.SimpleTrigger#MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT
     * @see org.quartz.SimpleTrigger#MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT
     * @see org.quartz.SimpleTrigger#MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT
     * @see org.quartz.CronTrigger#MISFIRE_INSTRUCTION_DO_NOTHING
     * @see org.quartz.CronTrigger#MISFIRE_INSTRUCTION_FIRE_ONCE_NOW
     */
    private int misfireInstruction = Trigger.MISFIRE_INSTRUCTION_SMART_POLICY;

    /**
     * 定时任务触发器类别
     */
    public enum TriggerType {
        /**
         * SimpleTrigger
         */
        SIMPLE,

        /**
         * CronTrigger
         */
        CRON
    }
}
