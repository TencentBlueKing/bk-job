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

package com.tencent.bk.job.execute.common.trace.executors;

import brave.Tracing;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public class TraceableExecutorService implements ExecutorService {

    protected ExecutorService delegateExecutorService;
    protected Tracing tracing;

    public TraceableExecutorService(ExecutorService executorService, Tracing tracing) {
        this.delegateExecutorService = executorService;
        this.tracing = tracing;
    }

    @Override
    public void shutdown() {
        delegateExecutorService.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegateExecutorService.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegateExecutorService.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegateExecutorService.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegateExecutorService.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return delegateExecutorService.submit(new TraceCallable(task, tracing));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return delegateExecutorService.submit(new TraceRunnable(task, tracing), result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return delegateExecutorService.submit(new TraceRunnable(task, tracing));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return delegateExecutorService.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout,
                                         TimeUnit unit) throws InterruptedException {
        return delegateExecutorService.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return delegateExecutorService.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout,
                           TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return delegateExecutorService.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        delegateExecutorService.execute(new TraceRunnable(command, tracing));
    }
}
