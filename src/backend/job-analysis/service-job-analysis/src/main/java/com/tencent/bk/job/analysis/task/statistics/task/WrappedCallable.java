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

@Slf4j
public class WrappedCallable<V> implements Callable<V>, Watchable<V> {

    protected String name = null;
    protected Callable<V> callable = null;
    protected TaskStatusListener<V> taskStatusListener = null;

    public WrappedCallable(String name, Callable<V> callable) {
        this.name = name;
        this.callable = callable;
    }

    @Override
    public TaskStatusListener<V> getTaskStatusListener() {
        return this.taskStatusListener;
    }

    @Override
    public void setTaskStatusListener(TaskStatusListener<V> listener) {
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
    public V call() throws Exception {
        try {
            if (taskStatusListener != null) {
                taskStatusListener.onStart();
            }
            // 任务5s内随机开始，避免扎堆DB瞬间高负载
            long waitMills = (long) (Math.random() * 5000);
            log.info("wait {}ms to start Task", waitMills);
            Thread.sleep(waitMills);
            V result = callable.call();
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
