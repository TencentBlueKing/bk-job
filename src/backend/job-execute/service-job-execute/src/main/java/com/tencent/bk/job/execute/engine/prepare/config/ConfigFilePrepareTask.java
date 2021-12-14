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

package com.tencent.bk.job.execute.engine.prepare.config;

import com.tencent.bk.job.common.util.Base64Util;
import com.tencent.bk.job.common.util.ListUtil;
import com.tencent.bk.job.execute.engine.prepare.JobTaskContext;
import com.tencent.bk.job.execute.engine.util.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 配置文件准备任务调度：从base64编码字符串生成文件
 */
@Slf4j
public class ConfigFilePrepareTask implements JobTaskContext {

    private final Long stepInstanceId;
    private final boolean isForRetry;
    private final List<List<Pair<String, String>>> configFileSourceList;
    private final ConfigFilePrepareTaskResultHandler resultHandler;
    private final List<Future<Boolean>> futureList = new ArrayList<>();
    public static ThreadPoolExecutor threadPoolExecutor = null;
    public static FinalResultHandler finalResultHandler = null;
    /**
     * 同步锁
     */
    volatile AtomicBoolean isReadyForNextStepWrapper = new AtomicBoolean(false);

    public static void init(int concurrency) {
        if (threadPoolExecutor == null) {
            threadPoolExecutor = new ThreadPoolExecutor(
                concurrency,
                concurrency,
                180L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(200),
                (r, executor) -> {
                    //使用请求的线程直接准备配置文件
                    log.error(
                        "prepare config file runnable rejected," +
                            " use current thread({}), plz add more threads",
                        Thread.currentThread().getName());
                    r.run();
                });
        }
    }

    public ConfigFilePrepareTask(Long stepInstanceId,
                                 boolean isForRetry,
                                 ConfigFilePrepareTaskResultHandler resultHandler,
                                 List<List<Pair<String, String>>> configFileSourceList
    ) {
        this.stepInstanceId = stepInstanceId;
        this.isForRetry = isForRetry;
        this.resultHandler = resultHandler;
        this.configFileSourceList = configFileSourceList;
        init(3);
    }

    public boolean isReadyForNext() {
        return this.isReadyForNextStepWrapper.get();
    }

    @Override
    public boolean isForRetry() {
        return isForRetry;
    }

    public void stop() {
        for (Future<Boolean> future : futureList) {
            if (!future.isDone()) {
                future.cancel(true);
            }
        }
        if (finalResultHandler != null) {
            finalResultHandler.interrupt();
        }
    }

    public void execute() {
        for (List<Pair<String, String>> configFileList : configFileSourceList) {
            for (Pair<String, String> pair : configFileList) {
                String fullFilePath = pair.getLeft();
                String base64Content = pair.getRight();
                GenerateFileTask task = new GenerateFileTask(stepInstanceId, fullFilePath, base64Content);
                Future<Boolean> future = threadPoolExecutor.submit(task);
                futureList.add(future);
            }
        }
        finalResultHandler = new FinalResultHandler(this, futureList, resultHandler);
        finalResultHandler.start();
    }

    class FinalResultHandler extends Thread {

        ConfigFilePrepareTask configFilePrepareTask;
        List<Future<Boolean>> futureList;
        ConfigFilePrepareTaskResultHandler resultHandler;

        FinalResultHandler(
            ConfigFilePrepareTask configFilePrepareTask,
            List<Future<Boolean>> futureList,
            ConfigFilePrepareTaskResultHandler resultHandler
        ) {
            this.configFilePrepareTask = configFilePrepareTask;
            this.futureList = futureList;
            this.resultHandler = resultHandler;
        }

        @Override
        public void run() {
            List<Boolean> resultList = new ArrayList<>();
            for (Future<Boolean> future : futureList) {
                try {
                    resultList.add(future.get(30, TimeUnit.MINUTES));
                } catch (InterruptedException e) {
                    log.info("[{}]:task stopped", stepInstanceId);
                    resultHandler.onStopped(configFilePrepareTask);
                    return;
                } catch (ExecutionException e) {
                    log.info("[{}]:task download failed", stepInstanceId);
                    resultHandler.onFailed(configFilePrepareTask);
                    return;
                } catch (TimeoutException e) {
                    log.info("[{}]:task download timeout", stepInstanceId);
                    resultHandler.onFailed(configFilePrepareTask);
                    return;
                }
            }
            if (ListUtil.isAllTrue(resultList)) {
                log.info("[{}]:all configFile prepare tasks success", stepInstanceId);
                resultHandler.onSuccess(configFilePrepareTask);
            } else {
                log.warn("[{}]:some configFile prepare tasks failed", stepInstanceId);
                resultHandler.onFailed(configFilePrepareTask);
            }
        }
    }

    static class GenerateFileTask implements Callable<Boolean> {

        private final Long stepInstanceId;
        private final String fullFilePath;
        private final String base64Content;

        GenerateFileTask(Long stepInstanceId, String fullFilePath, String base64Content) {
            this.stepInstanceId = stepInstanceId;
            this.fullFilePath = fullFilePath;
            this.base64Content = base64Content;
        }

        @Override
        public Boolean call() {
            try {
                byte[] contentBytes = Base64Util.decodeContentToByte(base64Content);
                File theFile = new File(fullFilePath);
                File parentDir = theFile.getParentFile();
                if (!parentDir.exists()) {
                    if (!parentDir.mkdirs()) {
                        log.warn(
                            "[{}]:Push config file, mkdir parent dir fail!dir:{}",
                            stepInstanceId,
                            parentDir.getAbsolutePath()
                        );
                        return false;
                    }
                    if (!parentDir.setWritable(true, false)) {
                        log.warn(
                            "[{}]:Push config file, set parent dir writeable fail!dir:{}",
                            stepInstanceId,
                            parentDir.getAbsolutePath()
                        );
                        return false;
                    }
                }
                if (theFile.exists() && theFile.isFile()) {
                    if (!theFile.delete()) {
                        log.warn(
                            "[{}]:Push config file, delete old file fail!dir:{}",
                            stepInstanceId,
                            theFile.getAbsolutePath()
                        );
                        return false;
                    }
                }

                if (!FileUtils.saveFileWithByte(fullFilePath, contentBytes)) {
                    log.warn(
                        "[{}]:Push config file, save file failed!fileName:{}",
                        stepInstanceId,
                        theFile.getAbsolutePath()
                    );
                    return false;
                }
            } catch (Exception e) {
                FormattingTuple msg = MessageFormatter.format(
                    "[{}]:Fail to generate configFile {}",
                    stepInstanceId,
                    fullFilePath
                );
                log.warn(msg.getMessage(), e);
                return false;
            }
            return true;
        }
    }
}
