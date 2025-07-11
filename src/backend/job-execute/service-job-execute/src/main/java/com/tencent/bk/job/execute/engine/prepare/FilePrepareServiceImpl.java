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

package com.tencent.bk.job.execute.engine.prepare;

import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.engine.prepare.local.DefaultLocalFilePrepareTaskResultHandler;
import com.tencent.bk.job.execute.engine.prepare.local.LocalFilePrepareService;
import com.tencent.bk.job.execute.engine.prepare.third.DefaultThirdFilePrepareTaskResultHandler;
import com.tencent.bk.job.execute.engine.prepare.third.ThirdFilePrepareService;
import com.tencent.bk.job.execute.engine.result.ResultHandleManager;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
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
    private final StepInstanceService stepInstanceService;

    @Autowired
    public FilePrepareServiceImpl(LocalFilePrepareService localFilePrepareService,
                                  ThirdFilePrepareService thirdFilePrepareService,
                                  TaskInstanceService taskInstanceService,
                                  TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher,
                                  ResultHandleManager resultHandleManager,
                                  StepInstanceService stepInstanceService) {
        this.localFilePrepareService = localFilePrepareService;
        this.thirdFilePrepareService = thirdFilePrepareService;
        this.taskInstanceService = taskInstanceService;
        this.taskExecuteMQEventDispatcher = taskExecuteMQEventDispatcher;
        this.resultHandleManager = resultHandleManager;
        this.stepInstanceService = stepInstanceService;
    }

    @Override
    public void clearPreparedTmpFile(long stepInstanceId) {
        localFilePrepareService.clearPreparedTmpFile(stepInstanceId);
        thirdFilePrepareService.clearPreparedTmpFile(stepInstanceId);
    }

    private void startPrepareLocalFileTask(StepInstanceDTO stepInstance,
                                           List<FileSourceDTO> fileSourceList,
                                           List<FilePrepareTaskResult> resultList,
                                           CountDownLatch latch
    ) {
        localFilePrepareService.prepareLocalFilesAsync(
            stepInstance,
            fileSourceList,
            new DefaultLocalFilePrepareTaskResultHandler(stepInstance, resultList, latch)
        );
    }

    private void startPrepareThirdFileTask(StepInstanceDTO stepInstance,
                                           List<FilePrepareTaskResult> resultList,
                                           CountDownLatch latch
    ) {
        thirdFilePrepareService.prepareThirdFileAsync(
            stepInstance,
            new DefaultThirdFilePrepareTaskResultHandler(stepInstance, resultList, latch)
        );
    }

    @Override
    public void prepareFileForGseTask(StepInstanceDTO stepInstance) {
        log.info("Begin to prepare source files for step, stepInstance: {}", stepInstance.getUniqueKey());
        List<FileSourceDTO> fileSourceList = stepInstance.getFileSourceList();
        if (CollectionUtils.isEmpty(fileSourceList)) {
            log.error("FileSource is empty, stepInstance: {}", stepInstance.getUniqueKey());
            return;
        }
        int taskCount = 0;
        boolean hasLocalFile = hasLocalFile(fileSourceList);
        boolean hasThirdFile = hasThirdFile(fileSourceList);
        List<String> prepareFileTypeList = new ArrayList<>();
        if (hasLocalFile) {
            taskCount += 1;
            prepareFileTypeList.add("localFile");
        }
        if (hasThirdFile) {
            taskCount += 1;
            prepareFileTypeList.add("thirdFile");
        }
        if (taskCount == 0) {
            // 没有需要准备文件的本地文件/第三方源文件
            log.warn("[{}]: no fileSource need to prepare", stepInstance.getUniqueKey());
            return;
        } else {
            log.info(
                "[{}]:{} kinds of file need to be prepared:{}",
                stepInstance.getUniqueKey(),
                prepareFileTypeList.size(),
                prepareFileTypeList
            );
        }
        CountDownLatch latch = new CountDownLatch(taskCount);
        final List<FilePrepareTaskResult> resultList = Collections.synchronizedList(new ArrayList<>(taskCount));
        if (hasLocalFile) {
            // 启动异步准备本地文件任务
            startPrepareLocalFileTask(stepInstance, fileSourceList, resultList, latch);
        }
        if (hasThirdFile) {
            // 启动异步准备第三方源文件任务
            startPrepareThirdFileTask(stepInstance, resultList, latch);
        }
        // 文件准备任务结果处理
        FilePrepareTaskResultHandler filePrepareTaskResultHandler = new DefaultFilePrepareTaskResultHandler(
            stepInstanceService,
            taskExecuteMQEventDispatcher
        );
        FilePrepareControlTask filePrepareControlTask =
            new FilePrepareControlTask(
                this,
                taskInstanceService,
                taskExecuteMQEventDispatcher, stepInstance,
                latch,
                resultList,
                filePrepareTaskResultHandler,
                stepInstanceService);
        resultHandleManager.handleDeliveredTask(filePrepareControlTask);
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

    @Override
    public void stopPrepareFile(StepInstanceDTO stepInstance) {
        localFilePrepareService.stopPrepareLocalFilesAsync(stepInstance);
        thirdFilePrepareService.stopPrepareThirdFileAsync(stepInstance);
    }

    @Override
    public boolean needToPrepareSourceFilesForGseTask(StepInstanceDTO stepInstance) {
        List<FileSourceDTO> fileSourceList = stepInstance.getFileSourceList();
        if (fileSourceList == null) {
            return false;
        }
        return hasLocalFile(fileSourceList) || hasThirdFile(fileSourceList);
    }
}
