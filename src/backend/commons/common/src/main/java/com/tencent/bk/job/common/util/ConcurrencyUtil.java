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

package com.tencent.bk.job.common.util;

import com.tencent.bk.job.common.exception.SubThreadException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.instrument.async.TraceableExecutorService;

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
 * @Description
 * @Date 2020/5/11
 * @Version 1.0
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
        ExecutorService threadPoolExecutor = new TraceableExecutorService(
            ApplicationContextRegister.getContext(),
            Executors.newFixedThreadPool(threadNum)
        );
        List<Output> result = getResultWithThreads(inputCollection, threadPoolExecutor, handler);
        threadPoolExecutor.shutdown();
        return result;
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
            Future<?> future = threadPoolExecutor.submit(new InnerTask<>(resultQueue, input,
                JobContextUtil.getRequestId(), handler));
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

    static class InnerTask<Input, Output> implements Runnable {
        //结果队列
        LinkedBlockingQueue<Output> resultQueue;
        Input input;
        String requestId;
        Handler<Input, Output> handler;

        InnerTask(LinkedBlockingQueue<Output> resultQueue, Input input, String requestId,
                  Handler<Input, Output> handler) {
            this.resultQueue = resultQueue;
            this.input = input;
            this.requestId = requestId;
            this.handler = handler;
        }

        @Override
        public void run() {
            JobContextUtil.setRequestId(requestId);
            try {
                resultQueue.addAll(handler.handle(input));
            } catch (Exception e) {
                log.error("InnerTask fail:", e);
            }
        }
    }
}
