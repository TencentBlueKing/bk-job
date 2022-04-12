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

package com.tencent.bk.job.analysis.task.statistics.task;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;

/**
 * 可观测的任务类型，任务各生命周期点可通知对应观察者处理相应事件
 *
 * @param <R> 任务执行完成后返回的结果类型
 */
@Slf4j
public class WatchableTask<R> implements Callable<R>, Watchable<R> {

    protected String name;
    protected Callable<R> task;
    protected TaskStatusListener<R> taskStatusListener = null;

    public WatchableTask(String name, Callable<R> task) {
        this.name = name;
        this.task = task;
    }

    @Override
    public TaskStatusListener<R> getTaskStatusListener() {
        return this.taskStatusListener;
    }

    @Override
    public void setTaskStatusListener(TaskStatusListener<R> listener) {
        this.taskStatusListener = listener;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public R call() throws Exception {
        try {
            if (taskStatusListener != null) {
                taskStatusListener.onStart();
            }
            // 任务5s内随机开始，避免扎堆DB瞬间高负载
            long waitMills = (long) (Math.random() * 5000);
            log.info("wait {}ms to start Task", waitMills);
            Thread.sleep(waitMills);
            R result = task.call();
            if (taskStatusListener != null) {
                taskStatusListener.onFinish(result);
            }
            return result;
        } catch (InterruptedException e) {
            log.info("task {} canceled", name);
        } catch (Throwable t) {
            log.error("task {} fail", name, t);
        }
        return null;
    }
}
