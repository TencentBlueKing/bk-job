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
import com.tencent.bk.job.execute.colddata.JobExecuteContextThreadLocalRepo;
import com.tencent.bk.job.execute.common.context.PropagatedJobExecuteContext;
import com.tencent.bk.job.execute.engine.listener.event.Event;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

@Slf4j
public abstract class BaseJobExecuteMqListener {


    public final void onEvent(Message<? extends Event> message) {
        beforeHandleMessage(message);
        try {
            handleEvent(message);
        } finally {
            afterHandle(message);
        }
    }

    private void beforeHandleMessage(Message<?> message) {
        log.info("beforeHandleMessage");
        MessageHeaders headers = message.getHeaders();
        String jobExecuteContextJson = (String) headers.get(PropagatedJobExecuteContext.KEY);
        if (StringUtils.isNotEmpty(jobExecuteContextJson)) {
            log.info("setJobExecuteContextThreadLocalRepo");
            JobExecuteContextThreadLocalRepo.set(JsonUtils.fromJson(jobExecuteContextJson,
                PropagatedJobExecuteContext.class));
        }
    }

    private void afterHandle(Message<?> message) {
        log.info("afterHandleMessage");
        JobExecuteContextThreadLocalRepo.unset();
    }

    protected abstract void handleEvent(Message<? extends Event> message);
}