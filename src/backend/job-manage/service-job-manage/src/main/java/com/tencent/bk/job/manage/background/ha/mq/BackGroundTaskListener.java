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

package com.tencent.bk.job.manage.background.ha.mq;

import com.tencent.bk.job.manage.background.ha.BackGroundTaskParser;
import com.tencent.bk.job.manage.background.ha.IBackGroundTask;
import com.tencent.bk.job.manage.background.ha.IBackGroundTaskExecutor;
import com.tencent.bk.job.manage.background.ha.TaskEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

/**
 * 后台任务监听器，用于监听从MQ接收到的任务并处理
 */
@Slf4j
@Service
public class BackGroundTaskListener {
    private final BackGroundTaskParser backGroundTaskParser;
    private final IBackGroundTaskExecutor backGroundTaskExecutor;

    @Autowired
    public BackGroundTaskListener(BackGroundTaskParser backGroundTaskParser,
                                  IBackGroundTaskExecutor backGroundTaskExecutor) {
        this.backGroundTaskParser = backGroundTaskParser;
        this.backGroundTaskExecutor = backGroundTaskExecutor;
    }

    public void handleTask(Message<TaskEntity> taskEntityMessage) {
        TaskEntity taskEntity = taskEntityMessage.getPayload();
        log.info("Received task from queue:{}", taskEntity);
        IBackGroundTask task = backGroundTaskParser.parse(taskEntity);
        if (task != null) {
            backGroundTaskExecutor.execute(task);
        }
    }
}
