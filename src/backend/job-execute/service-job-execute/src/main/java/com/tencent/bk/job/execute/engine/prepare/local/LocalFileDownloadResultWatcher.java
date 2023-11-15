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

package com.tencent.bk.job.execute.engine.prepare.local;

import com.tencent.bk.job.common.util.ListUtil;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class LocalFileDownloadResultWatcher implements Runnable {

    private final ArtifactoryLocalFilePrepareTask artifactoryLocalFilePrepareTask;
    private final StepInstanceDTO stepInstance;
    private final List<Future<Boolean>> futureList;
    private final LocalFilePrepareTaskResultHandler resultHandler;

    LocalFileDownloadResultWatcher(
        ArtifactoryLocalFilePrepareTask artifactoryLocalFilePrepareTask,
        StepInstanceDTO stepInstance,
        List<Future<Boolean>> futureList,
        LocalFilePrepareTaskResultHandler resultHandler
    ) {
        this.artifactoryLocalFilePrepareTask = artifactoryLocalFilePrepareTask;
        this.stepInstance = stepInstance;
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
                log.info("[{}]:task stopped", stepInstance.getUniqueKey());
                resultHandler.onStopped(artifactoryLocalFilePrepareTask);
                return;
            } catch (ExecutionException e) {
                log.info("[{}]:task download failed", stepInstance.getUniqueKey());
                resultHandler.onFailed(artifactoryLocalFilePrepareTask);
                return;
            } catch (TimeoutException e) {
                log.info("[{}]:task download timeout", stepInstance.getUniqueKey());
                resultHandler.onFailed(artifactoryLocalFilePrepareTask);
                return;
            }
        }
        if (ListUtil.isAllTrue(resultList)) {
            log.info("[{}]:All {} localFile download task(s) success", stepInstance.getUniqueKey(), futureList.size());
            resultHandler.onSuccess(artifactoryLocalFilePrepareTask);
        } else {
            int failCount = 0;
            for (Boolean result : resultList) {
                if (!result) {
                    failCount++;
                }
            }
            log.warn(
                "[{}]:{}/{} localFile download tasks failed",
                stepInstance.getUniqueKey(),
                failCount,
                resultList.size()
            );
            resultHandler.onFailed(artifactoryLocalFilePrepareTask);
        }
    }
}

