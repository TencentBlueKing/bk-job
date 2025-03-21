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

import com.tencent.bk.job.common.metrics.CommonMetricTags;
import com.tencent.bk.job.common.util.http.HttpConPoolUtil;
import com.tencent.bk.job.common.util.http.HttpResponse;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.engine.listener.event.JobMessage;
import com.tencent.bk.job.execute.engine.model.JobCallbackDTO;
import com.tencent.bk.job.execute.metrics.ExecuteMetricsConstants;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.slf4j.helpers.MessageFormatter;
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
        callbackDTO.setCallbackUrl(null);
        String bodyStr = JsonUtils.toJson(callbackDTO);
        boolean postJsonSuccess = false;
        try {
            log.info("Handle callback, taskInstanceId: {}, callbackDTO: {}", taskInstanceId, callbackDTO);
            validateUrl(callbackUrl);

            // 先用JSON数据格式请求一次
            HttpResponse response = HttpConPoolUtil.postJson(callbackUrl, bodyStr);

            if (response.getStatusCode() == HttpStatus.SC_OK) {
                postJsonSuccess = true;
                recordCallbackMetrics(
                    String.valueOf(response.getStatusCode()),
                    ExecuteMetricsConstants.TAG_VALUE_RESULT_POST_JSON_SUCCESS,
                    callbackDTO
                );
            } else {
                // 回调状态码不是200，用表单数据格式重试一次
                log.warn(
                    "Callback using json body failed, use form data to retry. TaskInstanceId={}, statusCode={}",
                    taskInstanceId,
                    response.getStatusCode()
                );
                response = HttpConPoolUtil.postFormData(callbackUrl, bodyStr);
                if (response.getStatusCode() == HttpStatus.SC_OK) {
                    recordCallbackMetrics(
                        String.valueOf(response.getStatusCode()),
                        ExecuteMetricsConstants.TAG_VALUE_RESULT_POST_FORM_DATA_SUCCESS,
                        callbackDTO
                    );
                } else {
                    // 回调状态码不是200，记录为失败
                    log.warn("Callback using form data failed, statusCode={}", response.getStatusCode());
                    recordCallbackMetrics(
                        String.valueOf(response.getStatusCode()),
                        ExecuteMetricsConstants.TAG_VALUE_RESULT_FAILED,
                        callbackDTO
                    );
                }
            }
            log.info(
                "Callback result: taskInstanceId={}, postJsonSuccess={}, httpStatusCode={}, result={}",
                taskInstanceId,
                postJsonSuccess,
                response.getStatusCode(),
                response.getEntity()
            );
        } catch (MalformedURLException e) {
            log.warn("Invalid callbackUrl: " + callbackUrl, e);
            recordCallbackMetrics(
                CommonMetricTags.VALUE_HTTP_STATUS_UNKNOWN,
                ExecuteMetricsConstants.TAG_VALUE_RESULT_FAILED,
                callbackDTO
            );
        } catch (Exception e) {
            String errorMsg = MessageFormatter.format(
                "Callback request failed, callbackUrl={}, bodyStr={}",
                callbackUrl,
                bodyStr
            ).getMessage();
            log.warn(errorMsg, e);
            recordCallbackMetrics(
                CommonMetricTags.VALUE_HTTP_STATUS_UNKNOWN,
                ExecuteMetricsConstants.TAG_VALUE_RESULT_FAILED,
                callbackDTO
            );
        }
    }

    /**
     * 校验URL是否合法
     */
    private void validateUrl(String callbackUrl) throws MalformedURLException {
        new URL(callbackUrl);
    }

    /**
     * 记录回调请求的监控指标
     */
    private void recordCallbackMetrics(String httpStatusCode,
                                       String result,
                                       JobCallbackDTO callbackDTO) {
        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(callbackDTO.getId());
        Iterable<Tag> tags = Tags.of(
            Tag.of(CommonMetricTags.KEY_APP_ID, String.valueOf(taskInstance.getAppId())),
            Tag.of(CommonMetricTags.KEY_APP_CODE, String.valueOf(taskInstance.getAppCode())),
            Tag.of(CommonMetricTags.KEY_HTTP_STATUS, String.valueOf(httpStatusCode)),
            Tag.of(ExecuteMetricsConstants.TAG_KEY_RESULT, String.valueOf(result))
        );
        meterRegistry.counter(ExecuteMetricsConstants.NAME_JOB_TASK_CALLBACK, tags).increment();
    }
}
