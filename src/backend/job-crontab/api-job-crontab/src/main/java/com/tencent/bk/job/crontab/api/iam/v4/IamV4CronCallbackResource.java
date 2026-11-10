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

package com.tencent.bk.job.crontab.api.iam.v4;

import com.tencent.bk.job.common.constant.JobCommonHeaders;
import com.tencent.bk.sdk.iamv4.callback.dto.request.V4CallbackRequestDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.response.V4CallbackResp;
import com.tencent.bk.sdk.iamv4.constants.IamV4Header;
import com.tentent.bk.job.common.api.feign.annotation.SmartFeignClient;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 权限中心 v4 回调 - 定时任务。
 *
 * <p>v4 全系统只有一个回调地址，权限中心的请求统一打到 job-manage，由其中的 dispatcher 按资源类型转发到
 * 本接口，故这里同时作为 Feign 客户端声明供 job-manage 调用。</p>
 */
@SmartFeignClient(value = "job-crontab", contextId = "iamV4CronCallbackResource")
public interface IamV4CronCallbackResource {

    @PostMapping("/iamv4/api/v1/resources/cron")
    ResponseEntity<V4CallbackResp<?>> callback(
        @Parameter(description = "租户ID，IAM回调时传入")
        @RequestHeader(value = JobCommonHeaders.BK_TENANT_ID, required = false) String tenantId,
        @Parameter(description = "IAM 回调认证头，Basic base64(bk_iam:token)")
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
        @Parameter(description = "IAM 回调请求ID，需原样回传")
        @RequestHeader(value = IamV4Header.X_REQUEST_ID, required = false) String requestId,
        @RequestBody V4CallbackRequestDTO callbackRequest);
}
