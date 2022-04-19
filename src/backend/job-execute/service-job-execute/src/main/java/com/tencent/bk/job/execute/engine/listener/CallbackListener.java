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

import com.tencent.bk.job.common.esb.model.EsbCallbackDTO;
import com.tencent.bk.job.common.util.http.HttpConPoolUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.engine.message.CallbackProcessor;
import com.tencent.bk.job.execute.engine.model.JobCallbackDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * 任务执行结束回调处理
 */
@Component
@EnableBinding({CallbackProcessor.class})
@Slf4j
public class CallbackListener {

    /**
     * 处理回调请求
     */
    @StreamListener(CallbackProcessor.INPUT)
    public void handleMessage(JobCallbackDTO callbackDTO) {
        long taskInstanceId = callbackDTO.getId();
        try {
            log.info("Handle callback, taskInstanceId: {}, msg: {}", taskInstanceId, callbackDTO);
            String callbackUrl = callbackDTO.getCallbackUrl();
            EsbCallbackDTO callback = callbackDTO.getCallback();
            String contentType;
            // callback优先
            if(callback != null && StringUtils.isNoneBlank(callback.getUrl())){
                callbackUrl = callback.getUrl();
                contentType = StringUtils.isBlank(callback.getContentType()) ? "application/json" : callback.getContentType();
            }else {
                contentType = "application/x-www-form-urlencoded";
            }
            try {
                new URL(callbackUrl);
            } catch (MalformedURLException var5) {
                log.error("Callback fail, bad url: {}", callbackUrl);
                return;
            }
            callbackDTO.setCallbackUrl(null);
            try {
                try {
                    String rst = HttpConPoolUtil.post(callbackUrl, JsonUtils.toJson(callbackDTO), contentType);
                    log.info("Callback success, taskInstanceId: {}, result: {}", taskInstanceId, rst);
                } catch (Throwable e) { //出错重试一次
                    String errorMsg = "Callback fail, taskInstanceId: " + taskInstanceId;
                    log.warn(errorMsg, e);
                    String rst = HttpConPoolUtil.post(callbackUrl, JsonUtils.toJson(callbackDTO), contentType);
                    log.info("Retry callback success, taskInstanceId: {}, result: {}", taskInstanceId, rst);
                }
            } catch (Throwable e) {
                String errorMsg = "Callback fail, taskInstanceId: " + taskInstanceId;
                log.warn(errorMsg, e);
            }
        } catch (Throwable e) {
            String errorMsg = "Callback fail, taskInstanceId: " + taskInstanceId;
            log.warn(errorMsg, e);
        }
    }
}
