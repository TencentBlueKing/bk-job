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

import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.metrics.CommonMetricTags;
import com.tencent.bk.job.common.util.http.HttpConPoolUtil;
import com.tencent.bk.job.common.util.http.HttpResponse;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.engine.listener.event.JobMessage;
import com.tencent.bk.job.execute.engine.model.JobCallbackDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * 任务执行结束回调处理
 */
@Component
@Slf4j
public class CallbackListener extends BaseJobMqListener {
    private final MeterRegistry meterRegistry;
    private final TaskInstanceService taskInstanceService;
    private final String UNKNOWN_HTTP_CODE = "unknown";

    public CallbackListener(MeterRegistry meterRegistry,
                            TaskInstanceService taskInstanceService) {
        this.meterRegistry = meterRegistry;
        this.taskInstanceService = taskInstanceService;
    }

    /**
     * 处理回调请求
     */
    @Override
    public void handleEvent(Message<? extends JobMessage> message) {
        JobCallbackDTO callbackDTO = (JobCallbackDTO) message.getPayload();
        long taskInstanceId = callbackDTO.getId();
        String callbackUrl = callbackDTO.getCallbackUrl();

        try {
            log.info("Handle callback, taskInstanceId: {}, msg: {}", taskInstanceId, callbackDTO);
            validateUrl(callbackUrl);

            HttpResponse response = callbackRequest(callbackUrl, callbackDTO);

            // 回调状态码不是200，重试一次
            if (response.getStatusCode() != HttpStatus.SC_OK) {
                log.warn("Callback failed, retrying. taskInstanceId: {}, statusCode: {}",
                    taskInstanceId,
                    response.getStatusCode());
                response = callbackRequest(callbackUrl, callbackDTO);
            }
            log.info("Final callback {}, taskInstanceId: {}, statusCode: {}, result: {}",
                    response.getStatusCode() == HttpStatus.SC_OK ? "success" : "fail",
                    taskInstanceId,
                    response.getStatusCode(),
                    response.getEntity());
        } catch (MalformedURLException e) {
            log.warn("Invalid callback URL: "+callbackUrl, e);
            recordCallbackMetrics(UNKNOWN_HTTP_CODE, callbackDTO);
        }
    }

    /**
     * 校验URL是否合法
     */
    private void validateUrl(String callbackUrl) throws MalformedURLException {
        new URL(callbackUrl);
    }

    /**
     * 执行回调请求
     */
    private HttpResponse callbackRequest(String callbackUrl, JobCallbackDTO callbackDTO) {
        try {
            callbackDTO.setCallbackUrl(null);
            HttpResponse response = HttpConPoolUtil.post(callbackUrl, JsonUtils.toJson(callbackDTO));
            recordCallbackMetrics(String.valueOf(response.getStatusCode()), callbackDTO);
            return response;
        } catch (Throwable e) {
            String errorMsg = String.format("Callback request failed, taskInstanceId: %s, url: %s",
                    callbackDTO.getId(),
                    callbackUrl);
            log.warn(errorMsg, e);
            recordCallbackMetrics(UNKNOWN_HTTP_CODE, callbackDTO);
            return new HttpResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, null);
        }
    }

    /**
     * 记录回调请求的监控指标
     */
    private void recordCallbackMetrics(String statusCode, JobCallbackDTO callbackDTO) {
        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(callbackDTO.getId());
        Iterable<Tag> tags = Tags.of(
            Tag.of(CommonMetricTags.KEY_APP_ID, String.valueOf(taskInstance.getAppId())),
            Tag.of(CommonMetricTags.KEY_APP_CODE, taskInstance.getAppCode()),
            Tag.of(CommonMetricTags.KEY_HTTP_STATUS, statusCode)
        );
        meterRegistry.counter(CommonMetricNames.TASK_CALLBACK_HTTP_STATUS, tags).increment();
    }
}
