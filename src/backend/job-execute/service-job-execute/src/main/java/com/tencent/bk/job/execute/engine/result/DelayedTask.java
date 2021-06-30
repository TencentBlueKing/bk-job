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

package com.tencent.bk.job.execute.engine.result;

import lombok.extern.slf4j.Slf4j;

import java.util.StringJoiner;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * 延时任务
 */
@Slf4j
public class DelayedTask implements Delayed, Task {
    /**
     * 到期时间，单位毫秒
     */
    private volatile long expireTime;
    /**
     * 延迟时间
     */
    private volatile long delayTimeMills;
    /**
     * 执行的任务
     */
    private volatile Task task;

    public DelayedTask() {
    }

    /**
     * DelayedTask Constructor
     *
     * @param task           任务
     * @param delayTimeMills 延迟时间，单位毫秒
     */
    public DelayedTask(Task task, long delayTimeMills) {
        this.task = task;
        long currentTime = System.currentTimeMillis();
        this.delayTimeMills = delayTimeMills;
        this.expireTime = currentTime + delayTimeMills;
    }

    @Override
    public void execute() {
        this.task.execute();
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long delayInMills = this.expireTime - System.currentTimeMillis();
        return unit.convert(delayInMills, TimeUnit.MILLISECONDS);
    }

    /**
     * 重置任务调度时间
     *
     * @param delayTimeMills 延迟时间，单位毫秒
     * @return 重置之后的延时任务
     */
    public DelayedTask reScheduled(long delayTimeMills) {
        this.expireTime = System.currentTimeMillis() + delayTimeMills;
        this.delayTimeMills = delayTimeMills;
        return this;
    }

    @Override
    public int compareTo(Delayed o) {
        if (o == this) {
            return 0;
        }
        long duration = (this.getDelay(TimeUnit.NANOSECONDS) - o.getDelay(TimeUnit.NANOSECONDS));
        return (duration == 0) ? 0 : ((duration < 0) ? -1 : 1);
    }

    public long getExpireTime() {
        return this.expireTime;
    }

    public Task getTask() {
        return this.task;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DelayedTask.class.getSimpleName() + "[", "]")
            .add("expireTime=" + expireTime)
            .add("delayTimeMills=" + delayTimeMills)
            .add("task=" + task)
            .toString();
    }

    @Override
    public String getTaskId() {
        return this.task.getTaskId();
    }
}
