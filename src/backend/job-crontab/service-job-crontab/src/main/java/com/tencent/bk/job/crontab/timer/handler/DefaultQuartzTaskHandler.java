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

package com.tencent.bk.job.crontab.timer.handler;

import com.tencent.bk.job.crontab.timer.AbstractQuartzTaskHandler;
import com.tencent.bk.job.crontab.timer.QuartzJob;
import org.apache.commons.collections4.CollectionUtils;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Set;

/**
 * QuartzTaskHandler 的默认实现
 **/
@Component
public class DefaultQuartzTaskHandler extends AbstractQuartzTaskHandler {

    @Autowired
    public DefaultQuartzTaskHandler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * 动态添加任务
     */
    @Override
    public void addJob(QuartzJob quartzJob) throws SchedulerException {
        Assert.notNull(quartzJob, "job cannot be empty!");
        Assert.notNull(quartzJob.getJobClass(), "job class cannot be empty!");
        Assert.notNull(quartzJob.getKey(), "Key cannot be empty!");
        Assert.notNull(quartzJob.getKey().getName(), "Key name cannot be empty!");

        JobDetail jobDetail = createJobDetail(quartzJob);

        Set<? extends Trigger> triggers = createTriggers(quartzJob);

        if (CollectionUtils.isEmpty(triggers)) {
            this.scheduler.addJob(jobDetail, false);
        } else {
            scheduler.scheduleJob(jobDetail, triggers, false);
        }
    }

    /**
     * 批量动态添加任务
     */
    @Override
    public void addJob(QuartzJob... quartzJobs) throws SchedulerException {
        Assert.notNull(quartzJobs, "jobs cannot be empty!");

        for (QuartzJob quartzJob : quartzJobs) {
            addJob(quartzJob);
        }
    }

    @Override
    public void deleteJob(JobKey jobKey) throws SchedulerException {
        Assert.notNull(jobKey, "jobKey cannot be empty!");
        Assert.notNull(jobKey.getName(), "jobKey name cannot be empty!");

        this.scheduler.deleteJob(jobKey);
    }

    @Override
    public void deleteJob(List<JobKey> jobKeys) throws SchedulerException {
        Assert.notNull(jobKeys, "jobKeys cannot be empty!");

        this.scheduler.deleteJobs(jobKeys);
    }

    /**
     * 暂停所有触发器
     */
    @Override
    public void pauseAll() throws SchedulerException {
        this.scheduler.pauseAll();
    }
}
