/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

package com.tencent.bk.job.common.util;

import com.tencent.bk.job.common.exception.SubThreadException;
import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 并发操作工具类
 */
@Slf4j
public class ConcurrencyUtil {

    /**
     * 使用多个线程并发获取结果
     *
     * @param inputCollection 输入集合
     * @param threadNum       线程数量
     * @param handler         单个输入处理器
     * @param <Input>         输入泛型
     * @param <Output>        单个输出元素泛型
     * @return 输出列表
     * @throws SubThreadException 子线程异常
     */
    public static <Input, Output> List<Output> getResultWithThreads(
        Collection<Input> inputCollection,
        int threadNum,
        Handler<Input, Output> handler
    ) throws SubThreadException {
        ExecutorService threadPoolExecutor = Executors.newFixedThreadPool(threadNum);
        try {
            return getResultWithThreads(inputCollection, threadPoolExecutor, handler);
        } finally {
            threadPoolExecutor.shutdown();
        }
    }

    /**
     * 使用多个线程并发获取结果
     *
     * @param inputCollection    输入集合
     * @param threadPoolExecutor 并发执行任务的线程池
     * @param handler            单个输入处理器
     * @param <Input>            输入泛型
     * @param <Output>           单个输出元素泛型
     * @return 输出列表
     * @throws SubThreadException 子线程异常
     */
    public static <Input, Output> List<Output> getResultWithThreads(
        Collection<Input> inputCollection,
        ExecutorService threadPoolExecutor,
        Handler<Input, Output> handler
    ) throws SubThreadException {
        LinkedBlockingQueue<Output> resultQueue = new LinkedBlockingQueue<>();
        List<Future<?>> futures = new ArrayList<>();
        for (Input input : inputCollection) {
            Future<?> future = threadPoolExecutor.submit(new InnerTask<>(resultQueue, input, handler));
            futures.add(future);
        }
        for (Future<?> future : futures) {
            try {
                future.get(5, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                throw new SubThreadException("sub thread interrupted", e);
            } catch (ExecutionException e) {
                String msg = "sub thread throws an exception";
                if (e.getCause() == null) {
                    throw new SubThreadException(msg, e);
                }
                throw new SubThreadException(msg, e.getCause());
            } catch (TimeoutException e) {
                String msg = "sub thread timed out 5 minutes";
                if (e.getCause() == null) {
                    throw new SubThreadException(msg, e);
                }
                throw new SubThreadException(msg, e.getCause());
            }
        }
        return new ArrayList<>(resultQueue);
    }

    public interface Handler<Input, Output> {
        Collection<Output> handle(Input input);
    }

    private static final ContextSnapshotFactory contextSnapshotFactory =
        ContextSnapshotFactory.builder().build();

    /**
     * 子任务包装：在提交线程捕获上下文（trace + 业务），在工作线程恢复，
     * 由 Micrometer Context Propagation 统一处理 ThreadLocal 传播。
     */
    static class InnerTask<Input, Output> implements Runnable {
        final LinkedBlockingQueue<Output> resultQueue;
        final Input input;
        final Handler<Input, Output> handler;
        final ContextSnapshot contextSnapshot;

        InnerTask(LinkedBlockingQueue<Output> resultQueue,
                  Input input,
                  Handler<Input, Output> handler) {
            this.resultQueue = resultQueue;
            this.input = input;
            this.handler = handler;
            this.contextSnapshot = contextSnapshotFactory.captureAll();
        }

        @Override
        public void run() {
            try (ContextSnapshot.Scope ignored = contextSnapshot.setThreadLocals()) {
                // 工作线程在恢复父线程上下文后，立即把 JobContext 替换为隔离副本，
                // 避免多个工作线程并发读写父线程同一份 JobContext 内的可变集合
                // (如 metricTagsMap 中的 ArrayList) 而抛出 ArrayIndexOutOfBoundsException。
                JobContextUtil.isolateContextForChildThread();
                resultQueue.addAll(handler.handle(input));
            } catch (Exception e) {
                log.error("InnerTask fail:", e);
            }
        }
    }
}
