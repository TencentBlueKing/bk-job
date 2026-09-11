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

package com.tencent.bk.job.manage.model.esb.v4.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.esb.model.EsbAppScopeReq;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * OpenAPI V4 同步执行方案请求体。
 *
 * <p>不接收模板版本号：服务层的版本参数是给页面做乐观锁用的（用户看到某个版本的差异对比后再确认），
 * API 调用方没有这个界面概念，且 v4 作业模板详情不暴露版本号，无从获取。本接口语义固定为「同步到模板当前最新版本」。
 */
@Getter
@Setter
public class V4SyncJobPlanRequest extends EsbAppScopeReq {

    /**
     * 执行方案 ID，必填且必须 > 0。
     */
    @JsonProperty("job_plan_id")
    @NotNull(message = "{validation.constraints.InvalidJobPlanId.message}")
    @Min(value = 1L, message = "{validation.constraints.InvalidJobPlanId.message}")
    private Long jobPlanId;
}
