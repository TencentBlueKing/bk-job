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
import org.quartz.JobKey;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class QuartzJobBuilder {

    private JobKey key;

    private String description;

    private Class<? extends AbstractQuartzJobBean> jobClass;

    private Map<String, Object> jobData = new HashMap<>();

    private Set<QuartzTrigger> triggers = new HashSet<>();

    private QuartzJobBuilder() {

    }

    public static QuartzJobBuilder newJob() {
        return new QuartzJobBuilder();
    }

    public static QuartzJobBuilder newJob(Class<? extends AbstractQuartzJobBean> jobClass) {
        QuartzJobBuilder builder = new QuartzJobBuilder();
        builder.forJob(jobClass);
        return builder;
    }

    public QuartzJob build() {

        QuartzJob job = new QuartzJob();

        job.setJobClass(jobClass);
        job.setDescription(description);

        if (key == null) {
            throw new InternalException(ErrorCode.INTERNAL_ERROR, "Job mast have key!");
        }

        job.setKey(key);

        if (!jobData.isEmpty()) {
            job.setJobData(jobData);
        }

        if (!this.triggers.isEmpty()) {
            job.setTriggers(this.triggers);
        }

        return job;
    }

    public QuartzJobBuilder withIdentity(JobKey jobKey) {
        this.key = jobKey;
        return this;
    }

    public QuartzJobBuilder withIdentity(String name, String group) {
        key = new JobKey(name, group);
        return this;
    }

    public QuartzJobBuilder withDescription(String jobDescription) {
        this.description = jobDescription;
        return this;
    }

    public QuartzJobBuilder forJob(Class<? extends AbstractQuartzJobBean> jobClazz) {
        this.jobClass = jobClazz;
        return this;
    }

    public QuartzJobBuilder withTrigger(QuartzTrigger trigger) {
        this.triggers.add(trigger);
        return this;
    }

    public QuartzJobBuilder usingJobData(String key, String value) {
        this.jobData.put(key, value);
        return this;
    }

}
