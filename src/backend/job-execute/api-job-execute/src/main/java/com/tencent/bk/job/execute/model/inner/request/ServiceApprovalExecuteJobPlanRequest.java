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

package com.tencent.bk.job.execute.model.inner.request;

import com.tencent.bk.job.execute.model.esb.v4.req.V4ExecuteJobPlanRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 带审批的启动执行方案请求（服务内部调用）。
 * <p>
 * 刻意不复用 {@link ServiceTaskExecuteRequest}：后者对校验失败是抛 InvalidParamException，
 * 且其 skipAuth 与 cronTaskId 语义服务于定时任务触发链路。本请求没有 cronTaskId，
 * 从结构上保证 skipAuth 不可能生效，鉴权必然真实执行。
 */
@Data
@Schema(description = "带审批的启动执行方案请求报文")
public class ServiceApprovalExecuteJobPlanRequest {

    @Schema(description = "原始 v4 启动执行方案请求", required = true)
    private V4ExecuteJobPlanRequest request;

    @Schema(description = "操作人，必须为审批任务的发起人", required = true)
    private String operator;

    @Schema(description = "发起方 appCode")
    private String appCode;

    @Schema(description = "是否只做预检，不产生任何副作用")
    private boolean dryRun;
}
