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

package com.tencent.bk.job.crontab.api.esb.v4;

import com.tencent.bk.job.common.annotation.EsbV4API;
import com.tencent.bk.job.common.constant.JobCommonHeaders;
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import com.tencent.bk.job.crontab.model.esb.v4.req.V4UpdateCronStatusRequest;
import com.tencent.bk.job.crontab.model.esb.v4.resp.V4CronJobDTO;
import com.tentent.bk.job.common.api.feign.annotation.SmartFeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * <b>类级 MVC 注解（@RestController / @RequestMapping）必须留在实现类上，不能挪到本接口</b>：
 * 本接口同时是 Feign 客户端，一旦接口上带这两个注解之一，Feign 代理会被
 * RequestMappingHandlerMapping 判定为 handler，在调用方服务上凭空注册出一个同路径的转发端点。
 */
@EsbV4API
@Validated
@SmartFeignClient(value = "job-crontab", contextId = "openApiUpdateCronStatusV4Resource")
public interface OpenApiUpdateCronStatusV4Resource {

    /**
     * @param dryRun 预检标识。为 true 时走完整校验与鉴权后即返回，不改定时任务状态、不动调度、
     *               不产生审计，响应以 dry_run_summary 回带解析出的操作概要
     */
    @PostMapping("/esb/api/v4/update_cron_status")
    EsbV4Response<V4CronJobDTO> updateCronStatus(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestHeader(value = JobCommonHeaders.BK_JOB_DRY_RUN, required = false) Boolean dryRun,
        @RequestBody
        @Validated
            V4UpdateCronStatusRequest request
    );
}
