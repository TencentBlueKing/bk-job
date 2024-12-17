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

package com.tencent.bk.job.execute.engine.listener;

import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.common.context.JobExecuteContext;
import com.tencent.bk.job.execute.common.context.JobExecuteContextThreadLocalRepo;
import com.tencent.bk.job.execute.engine.listener.event.JobMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

@Slf4j
public abstract class BaseJobMqListener {


    public final void onEvent(Message<? extends JobMessage> message) {
        beforeHandleMessage(message);
        try {
            handleEvent(message);
        } finally {
            afterHandle(message);
        }
    }

    private void beforeHandleMessage(Message<? extends JobMessage> message) {
        MessageHeaders headers = message.getHeaders();
        String jobExecuteContextJson = (String) headers.get(JobExecuteContext.KEY);
        if (StringUtils.isNotEmpty(jobExecuteContextJson)) {
            JobExecuteContextThreadLocalRepo.set(JsonUtils.fromJson(jobExecuteContextJson,
                JobExecuteContext.class));
        }
    }

    private void afterHandle(Message<? extends JobMessage> message) {
        JobExecuteContextThreadLocalRepo.unset();
    }

    protected abstract void handleEvent(Message<? extends JobMessage> message);
}