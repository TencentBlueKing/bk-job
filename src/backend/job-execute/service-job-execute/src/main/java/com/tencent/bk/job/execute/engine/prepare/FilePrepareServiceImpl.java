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

package com.tencent.bk.job.execute.engine.prepare;

import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.engine.listener.event.EventSource;
import com.tencent.bk.job.execute.engine.listener.event.GseTaskEvent;
import com.tencent.bk.job.execute.engine.listener.event.JobEvent;
import com.tencent.bk.job.execute.engine.listener.event.StepEvent;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.engine.prepare.local.LocalFilePrepareService;
import com.tencent.bk.job.execute.engine.prepare.local.LocalFilePrepareTaskResultHandler;
import com.tencent.bk.job.execute.engine.prepare.third.ThirdFilePrepareService;
import com.tencent.bk.job.execute.engine.prepare.third.ThirdFilePrepareTaskResultHandler;
import com.tencent.bk.job.execute.engine.result.ResultHandleManager;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * 汇总控制多种文件准备任务：本地文件、第三方源文件
 */
@Slf4j
@Primary
@Component
public class FilePrepareServiceImpl implements FilePrepareService {
    private final LocalFilePrepareService localFilePrepareService;
    private final ThirdFilePrepareService thirdFilePrepareService;
    private final TaskInstanceService taskInstanceService;
    private final TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher;
    private final ResultHandleManager resultHandleManager;

    @Autowired
    public FilePrepareServiceImpl(
        LocalFilePrepareService localFilePrepareService,
        ThirdFilePrepareService thirdFilePrepareService,
        TaskInstanceService taskInstanceService,
        TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher,
        ResultHandleManager resultHandleManager) {
        this.localFilePrepareService = localFilePrepareService;
        this.thirdFilePrepareService = thirdFilePrepareService;
        this.taskInstanceService = taskInstanceService;
        this.taskExecuteMQEventDispatcher = taskExecuteMQEventDispatcher;
        this.resultHandleManager = resultHandleManager;
    }

    @Override
    public void retryPrepareFile(long stepInstanceId) {
        // TODO
    }

    @Override
    public void clearPreparedTmpFile(long stepInstanceId) {
        localFilePrepareService.clearPreparedTmpFile(stepInstanceId);
        thirdFilePrepareService.clearPreparedTmpFile(stepInstanceId);
    }

    private boolean hasLocalFile(List<FileSourceDTO> fileSourceList) {
        for (FileSourceDTO fileSourceDTO : fileSourceList) {
            if (fileSourceDTO.isLocalUpload()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasThirdFile(List<FileSourceDTO> fileSourceList) {
        for (FileSourceDTO fileSourceDTO : fileSourceList) {
            if (fileSourceDTO.getFileSourceId() != null && fileSourceDTO.getFileSourceId() > 0) {
                return true;
            }
        }
        return false;
    }

    private void startPrepareLocalFileTask(long stepInstanceId,
                                           List<FileSourceDTO> fileSourceList,
                                           List<FilePrepareTaskResult> resultList,
                                           CountDownLatch latch
    ) {
        localFilePrepareService.prepareLocalFilesAsync(
            stepInstanceId,
            fileSourceList,
            new LocalFilePrepareTaskResultHandler() {
                @Override
                public void onSuccess(JobTaskContext taskContext) {
                    log.info("stepInstanceId={},LocalFilePrepareTask success", stepInstanceId);
                    resultList.add(new FilePrepareTaskResult(FilePrepareTaskResult.STATUS_SUCCESS, taskContext));
                    latch.countDown();
                }

                @Override
                public void onStopped(JobTaskContext taskContext) {
                    log.info("stepInstanceId={},LocalFilePrepareTask stopped", stepInstanceId);
                    resultList.add(new FilePrepareTaskResult(FilePrepareTaskResult.STATUS_STOPPED, taskContext));
                    latch.countDown();
                }

                @Override
                public void onFailed(JobTaskContext taskContext) {
                    log.warn("stepInstanceId={},LocalFilePrepareTask failed", stepInstanceId);
                    resultList.add(new FilePrepareTaskResult(FilePrepareTaskResult.STATUS_FAILED, taskContext));
                    latch.countDown();
                }
            });
    }

    private void startPrepareThirdFileTask(StepInstanceDTO stepInstance,
                                           List<FilePrepareTaskResult> resultList,
                                           CountDownLatch latch
    ) {
        thirdFilePrepareService.prepareThirdFileAsync(
            stepInstance,
            new ThirdFilePrepareTaskResultHandler() {

                @Override
                public void onSuccess(JobTaskContext taskContext) {
                    log.info("stepInstanceId={},ThirdFilePrepareTask success", stepInstance.getId());
                    resultList.add(new FilePrepareTaskResult(FilePrepareTaskResult.STATUS_SUCCESS, taskContext));
                    latch.countDown();
                }

                @Override
                public void onStopped(JobTaskContext taskContext) {
                    log.info("stepInstanceId={},ThirdFilePrepareTask stopped", stepInstance.getId());
                    resultList.add(new FilePrepareTaskResult(FilePrepareTaskResult.STATUS_STOPPED, taskContext));
                    latch.countDown();
                }

                @Override
                public void onFailed(JobTaskContext taskContext) {
                    log.warn("stepInstanceId={},ThirdFilePrepareTask failed", stepInstance.getId());
                    resultList.add(new FilePrepareTaskResult(FilePrepareTaskResult.STATUS_FAILED, taskContext));
                    latch.countDown();
                }
            }
        );
    }

    @Override
    public void prepareFileForGseTask(long stepInstanceId) {
        StepInstanceDTO stepInstance = taskInstanceService.getStepInstanceDetail(stepInstanceId);
        List<FileSourceDTO> fileSourceList = stepInstance.getFileSourceList();
        if (fileSourceList == null) {
            log.warn("stepInstanceId={},fileSourceList is null", stepInstanceId);
            // TODO-Rolling
            taskExecuteMQEventDispatcher.dispatchGseTaskEvent(GseTaskEvent.startGseTask(stepInstance.getId(), null));
            return;
        }
        int taskCount = 0;
        boolean hasLocalFile = hasLocalFile(fileSourceList);
        boolean hasThirdFile = hasThirdFile(fileSourceList);
        if (hasLocalFile) taskCount += 1;
        if (hasThirdFile) taskCount += 1;
        if (taskCount == 0) {
            // 没有需要准备文件的本地文件/第三方源文件
            // TODO-Rolling
            taskExecuteMQEventDispatcher.dispatchGseTaskEvent(GseTaskEvent.startGseTask(stepInstance.getId(), null));
            return;
        }
        log.debug("stepInstanceId={},prepareTaskCount={}", stepInstanceId, taskCount);
        CountDownLatch latch = new CountDownLatch(taskCount);
        final List<FilePrepareTaskResult> resultList = Collections.synchronizedList(new ArrayList<>(taskCount));
        if (hasLocalFile) {
            // 启动异步准备本地文件任务
            startPrepareLocalFileTask(stepInstanceId, fileSourceList, resultList, latch);
        }
        if (hasThirdFile) {
            // 启动异步准备第三方源文件任务
            startPrepareThirdFileTask(stepInstance, resultList, latch);
        }
        // 文件准备任务结果处理
        FilePrepareTaskResultHandler filePrepareTaskResultHandler = new FilePrepareTaskResultHandler() {
            @Override
            public void onFinished(StepInstanceDTO stepInstance, List<FilePrepareTaskResult> resultList) {
                log.info("stepInstanceId={},prepareTask finished", stepInstance.getId());
                handleFinalTaskResult(resultList, stepInstance);
            }

            @Override
            public void onTimeout(StepInstanceDTO stepInstance) {
                log.info("stepInstanceId={},prepareTask timeout", stepInstance.getId());
                onFailed(stepInstance, null);
            }

            @Override
            public void onException(StepInstanceDTO stepInstance, Throwable t) {
                FormattingTuple msg = MessageFormatter.format(
                    "stepInstanceId={},prepareTask exception",
                    stepInstance.getId()
                );
                log.error(msg.getMessage(), t);
                onFailed(stepInstance, null);
            }
        };
        FilePrepareControlTask filePrepareControlTask =
            new FilePrepareControlTask(
                this,
                taskInstanceService,
                stepInstance,
                latch,
                resultList,
                filePrepareTaskResultHandler
            );
        resultHandleManager.handleDeliveredTask(filePrepareControlTask);
    }

    @Override
    public void stopPrepareFile(long stepInstanceId) {
        localFilePrepareService.stopPrepareLocalFilesAsync(stepInstanceId);
        thirdFilePrepareService.stopPrepareThirdFileAsync(stepInstanceId);
    }

    private FilePrepareTaskResult combineTaskResult(List<FilePrepareTaskResult> resultList) {
        int size = resultList.size();
        if (size == 0) return null;
        if (size == 1) return resultList.get(0);
        for (FilePrepareTaskResult filePrepareTaskResult : resultList) {
            if (filePrepareTaskResult.status == FilePrepareTaskResult.STATUS_FAILED) {
                return filePrepareTaskResult;
            }
        }
        for (FilePrepareTaskResult filePrepareTaskResult : resultList) {
            if (filePrepareTaskResult.status == FilePrepareTaskResult.STATUS_STOPPED) {
                return filePrepareTaskResult;
            }
        }
        return resultList.get(0);
    }

    private void handleFinalTaskResult(List<FilePrepareTaskResult> resultList, StepInstanceDTO stepInstance) {
        FilePrepareTaskResult finalResult = combineTaskResult(resultList);
        if (finalResult == null) return;
        if (finalResult.status == FilePrepareTaskResult.STATUS_SUCCESS) {
            onSuccess(stepInstance, finalResult);
        } else if (finalResult.status == FilePrepareTaskResult.STATUS_STOPPED) {
            onStopped(stepInstance, finalResult);
        } else if (finalResult.status == FilePrepareTaskResult.STATUS_FAILED) {
            onFailed(stepInstance, finalResult);
        } else {
            log.warn("Unknown status:{}", finalResult.status);
        }
    }

    private void onSuccess(StepInstanceDTO stepInstance, FilePrepareTaskResult finalResult) {
        if (!finalResult.getTaskContext().isForRetry()) {
            // 直接进行下一步
            taskExecuteMQEventDispatcher.dispatchStepEvent(StepEvent.continueGseFileStep(stepInstance.getId()));
        }
    }

    private void onStopped(StepInstanceDTO stepInstance, FilePrepareTaskResult finalResult) {
        // 步骤状态变更
        taskInstanceService.updateStepStatus(stepInstance.getId(), RunStatusEnum.STOP_SUCCESS.getValue());
        // 任务状态变更
        taskExecuteMQEventDispatcher.dispatchJobEvent(
            JobEvent.refreshJob(stepInstance.getTaskInstanceId(),
                EventSource.buildStepEventSource(stepInstance.getId())));
    }

    private void onFailed(StepInstanceDTO stepInstance, FilePrepareTaskResult finalResult) {
        // 文件源文件下载失败
        taskInstanceService.updateStepStatus(stepInstance.getId(), RunStatusEnum.FAIL.getValue());
        taskExecuteMQEventDispatcher.dispatchJobEvent(
            JobEvent.refreshJob(stepInstance.getTaskInstanceId(),
                EventSource.buildStepEventSource(stepInstance.getId())));
    }
}
