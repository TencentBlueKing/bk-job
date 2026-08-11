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

package com.tencent.bk.job.manage.model.inner.request;

import com.tencent.bk.job.manage.model.esb.v4.req.V4CreateJobPlanRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 带审批的创建执行方案请求（服务内部调用）。
 * <p>
 * 入参直接包装原始 v4 请求体，转换与校验在 job-manage 侧由 V4JobPlanCreateService 完成，
 * 与 OpenAPI 直接创建走同一段代码，只切换 dryRun 取值。
 */
@Data
@Schema(description = "带审批的创建执行方案请求报文")
public class ServiceApprovalCreateJobPlanRequest {

    @Schema(description = "原始 v4 创建执行方案请求", required = true)
    private V4CreateJobPlanRequest request;

    /**
     * 操作人。inner 接口不经过网关的身份校验，操作人由调用方传入；
     * job-analysis 必须传审批任务里存的 creator，且该值只能取自 DB，不能取自当次请求
     */
    @Schema(description = "操作人，必须为审批任务的发起人", required = true)
    private String operator;

    @Schema(description = "发起方 appCode")
    private String appCode;

    /**
     * 是否只做预检。true 用于创建审批任务时的校验，false 用于审批通过后的放行创建
     */
    @Schema(description = "是否只做预检，不产生任何副作用")
    private boolean dryRun;
}
