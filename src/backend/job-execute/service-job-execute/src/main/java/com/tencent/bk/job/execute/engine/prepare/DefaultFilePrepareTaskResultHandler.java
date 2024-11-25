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
import com.tencent.bk.job.execute.engine.listener.event.JobEvent;
import com.tencent.bk.job.execute.engine.listener.event.StepEvent;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.service.StepInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.util.List;

@Slf4j
public class DefaultFilePrepareTaskResultHandler implements FilePrepareTaskResultHandler {

    private final StepInstanceService stepInstanceService;
    private final TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher;

    public DefaultFilePrepareTaskResultHandler(StepInstanceService stepInstanceService,
                                               TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher) {
        this.stepInstanceService = stepInstanceService;
        this.taskExecuteMQEventDispatcher = taskExecuteMQEventDispatcher;
    }

    @Override
    public void onFinished(StepInstanceDTO stepInstance, List<FilePrepareTaskResult> resultList) {
        log.info("[{}]: prepareTask finished", stepInstance.getUniqueKey());
        handleFinalTaskResult(resultList, stepInstance);
    }

    @Override
    public void onTimeout(StepInstanceDTO stepInstance) {
        log.info("[{}]: prepareTask timeout", stepInstance.getUniqueKey());
        onFailed(stepInstance, null);
    }

    @Override
    public void onException(StepInstanceDTO stepInstance, Throwable t) {
        FormattingTuple msg = MessageFormatter.format(
            "[{}]: prepareTask exception",
            stepInstance.getUniqueKey()
        );
        log.error(msg.getMessage(), t);
        onFailed(stepInstance, null);
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

    private void onSuccess(StepInstanceDTO stepInstance, FilePrepareTaskResult finalResult) {
        if (!finalResult.getTaskContext().isForRetry()) {
            // 直接进行下一步
            taskExecuteMQEventDispatcher.dispatchStepEvent(
                StepEvent.continueGseFileStep(stepInstance.getTaskInstanceId(), stepInstance.getId()));
        }
    }

    private void onStopped(StepInstanceDTO stepInstance, FilePrepareTaskResult finalResult) {
        // 步骤状态变更
        stepInstanceService.updateStepStatus(stepInstance.getTaskInstanceId(),
            stepInstance.getId(), RunStatusEnum.STOP_SUCCESS.getValue());
        // 任务状态变更
        taskExecuteMQEventDispatcher.dispatchJobEvent(
            JobEvent.refreshJob(stepInstance.getTaskInstanceId(),
                EventSource.buildStepEventSource(stepInstance.getTaskInstanceId(), stepInstance.getId())));
    }

    private void onFailed(StepInstanceDTO stepInstance, FilePrepareTaskResult finalResult) {
        // 文件源文件下载失败
        stepInstanceService.updateStepStatus(stepInstance.getTaskInstanceId(),
            stepInstance.getId(), RunStatusEnum.FAIL.getValue());
        taskExecuteMQEventDispatcher.dispatchJobEvent(
            JobEvent.refreshJob(stepInstance.getTaskInstanceId(),
                EventSource.buildStepEventSource(stepInstance.getTaskInstanceId(), stepInstance.getId())));
    }

}
