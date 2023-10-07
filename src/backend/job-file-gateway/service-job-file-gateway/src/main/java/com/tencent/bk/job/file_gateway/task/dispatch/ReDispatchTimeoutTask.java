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

package com.tencent.bk.job.file_gateway.task.dispatch;

import com.tencent.bk.job.file_gateway.consts.TaskStatusEnum;
import com.tencent.bk.job.file_gateway.dao.filesource.FileTaskDAO;
import com.tencent.bk.job.file_gateway.service.ReDispatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ReDispatchTimeoutTask {

    private final FileTaskDAO fileTaskDAO;
    private final ReDispatchService reDispatchService;

    @Value("${job.file-gateway.reDispatch.timeoutTask.enabled:true}")
    private boolean reDispatchTimeoutTaskEnabled = true;

    @Value("${job.file-gateway.reDispatch.timeoutTask.timeoutSeconds:10}")
    private int reDispatchTaskTimeoutSeconds = 10;

    @Autowired
    public ReDispatchTimeoutTask(FileTaskDAO fileTaskDAO,
                                 ReDispatchService reDispatchService) {
        this.fileTaskDAO = fileTaskDAO;
        this.reDispatchService = reDispatchService;
    }

    public void run() {
        if (!reDispatchTimeoutTaskEnabled) {
            log.info("reDispatch timeout task not enabled, you can config it in configuration file by set job" +
                ".file-gateway.reDispatch.timeoutTask.enabled=true");
            return;
        }
        // 找出未结束且长时间无响应的任务，无响应且未结束的任务就应当被重调度了
        long fileSourceTaskStatusExpireTimeMills = reDispatchTaskTimeoutSeconds * 1000L;
        List<String> timeoutFileSourceTaskIdList = fileTaskDAO.listTimeoutFileSourceTaskIds(
            fileSourceTaskStatusExpireTimeMills,
            TaskStatusEnum.getRunningStatusSet(),
            0,
            -1
        );
        // 进行超时重调度
        for (String fileSourceTaskId : timeoutFileSourceTaskIdList) {
            boolean result = reDispatchService.reDispatchByGateway(fileSourceTaskId, 0L, 5000L);
            log.info(
                "reDispatch fileSourceTask by timeout({}s):{}, result={}",
                reDispatchTaskTimeoutSeconds,
                fileSourceTaskId,
                result
            );
        }
    }
}
