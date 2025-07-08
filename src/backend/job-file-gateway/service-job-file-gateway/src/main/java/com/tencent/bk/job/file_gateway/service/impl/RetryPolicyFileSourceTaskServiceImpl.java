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

package com.tencent.bk.job.file_gateway.service.impl;

import com.tencent.bk.job.file_gateway.model.resp.inner.TaskInfoDTO;
import com.tencent.bk.job.file_gateway.service.FileSourceTaskService;
import com.tencent.bk.job.file_gateway.service.RetryPolicyFileSourceTaskService;
import com.tencent.bk.job.file_gateway.service.context.impl.FileSourceTaskRetryContext;
import com.tencent.bk.job.file_gateway.service.retry.FileSourceTaskRetryPolicy;
import com.tencent.bk.job.file_gateway.service.retry.impl.ExceptionRetryPolicy;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class RetryPolicyFileSourceTaskServiceImpl implements RetryPolicyFileSourceTaskService {

    private final FileSourceTaskService fileSourceTaskService;
    private final FileSourceTaskRetryPolicy retryPolicy = new ExceptionRetryPolicy(3, 5000);

    @Autowired
    public RetryPolicyFileSourceTaskServiceImpl(FileSourceTaskService fileSourceTaskService) {
        this.fileSourceTaskService = fileSourceTaskService;
    }

    @Override
    public TaskInfoDTO startFileSourceDownloadTask(String username,
                                                   Long appId,
                                                   Long stepInstanceId,
                                                   Integer executeCount,
                                                   String batchTaskId,
                                                   Integer fileSourceId,
                                                   List<String> filePathList) {
        int retryCount = 0;
        boolean shouldRetry;
        do {
            try {
                return fileSourceTaskService.startFileSourceDownloadTask(
                    username,
                    appId,
                    stepInstanceId,
                    executeCount,
                    batchTaskId,
                    fileSourceId,
                    filePathList
                );
            } catch (Exception e) {
                retryCount += 1;
                FileSourceTaskRetryContext retryContext = new FileSourceTaskRetryContext(e);
                shouldRetry = retryPolicy.shouldRetry(retryContext, retryCount);
                if (shouldRetry) {
                    String msg = MessageFormatter.arrayFormat(
                        "Fail to startFileSourceDownloadTask, stepInstanceId={}, " +
                            "executeCount={}, batchTaskId={}, retry {}",
                        new Object[]{
                            stepInstanceId,
                            executeCount,
                            batchTaskId,
                            retryCount
                        }
                    ).getMessage();
                    log.info(msg, e);
                } else {
                    throw e;
                }
            }
        } while (true);
    }
}
