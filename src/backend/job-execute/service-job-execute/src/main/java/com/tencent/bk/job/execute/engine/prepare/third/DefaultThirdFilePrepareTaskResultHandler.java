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

package com.tencent.bk.job.execute.engine.prepare.third;

import com.tencent.bk.job.execute.engine.prepare.FilePrepareTaskResult;
import com.tencent.bk.job.execute.engine.prepare.JobTaskContext;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class DefaultThirdFilePrepareTaskResultHandler implements ThirdFilePrepareTaskResultHandler {

    private final StepInstanceDTO stepInstance;
    private final List<FilePrepareTaskResult> resultList;
    private final CountDownLatch latch;

    public DefaultThirdFilePrepareTaskResultHandler(StepInstanceDTO stepInstance,
                                                    List<FilePrepareTaskResult> resultList,
                                                    CountDownLatch latch) {
        this.stepInstance = stepInstance;
        this.resultList = resultList;
        this.latch = latch;
    }

    @Override
    public void onSuccess(JobTaskContext taskContext) {
        log.info("[{}]: ThirdFilePrepareTask success", stepInstance.getUniqueKey());
        resultList.add(new FilePrepareTaskResult(FilePrepareTaskResult.STATUS_SUCCESS, taskContext));
        latch.countDown();
    }

    @Override
    public void onStopped(JobTaskContext taskContext) {
        log.info("[{}]: ThirdFilePrepareTask stopped", stepInstance.getUniqueKey());
        resultList.add(new FilePrepareTaskResult(FilePrepareTaskResult.STATUS_STOPPED, taskContext));
        latch.countDown();
    }

    @Override
    public void onFailed(JobTaskContext taskContext) {
        log.warn("[{}]: ThirdFilePrepareTask failed", stepInstance.getUniqueKey());
        resultList.add(new FilePrepareTaskResult(FilePrepareTaskResult.STATUS_FAILED, taskContext));
        latch.countDown();
    }
}
