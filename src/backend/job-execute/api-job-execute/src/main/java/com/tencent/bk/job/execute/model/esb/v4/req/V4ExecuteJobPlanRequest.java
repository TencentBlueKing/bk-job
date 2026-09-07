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

package com.tencent.bk.job.execute.model.esb.v4.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.esb.model.EsbAppScopeReq;
import com.tencent.bk.job.execute.validate.ValidCallbackUrl;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.Valid;
import java.util.List;

/**
 * v4 启动执行方案请求。
 * <p>
 * 与 v3 的 EsbExecuteJobV3Request 的协议差异：
 * <ul>
 *     <li>不再提供 bk_biz_id 兼容字段，业务范围只用 bk_scope_type + bk_scope_id；</li>
 *     <li>主机类全局变量的取值用 v4 的 {@link V4ExecuteTargetDTO}，支持容器执行对象。</li>
 * </ul>
 */
@Getter
@Setter
public class V4ExecuteJobPlanRequest extends EsbAppScopeReq {

    /**
     * 执行方案 ID
     */
    @JsonProperty("job_plan_id")
    private Long planId;

    /**
     * 全局变量取值列表，不传表示全部用执行方案里的默认值
     */
    @JsonProperty("global_var_list")
    @Valid
    private List<V4GlobalVarDTO> globalVars;

    /**
     * 任务执行完成后的回调 url
     */
    @JsonProperty("callback_url")
    @ValidCallbackUrl
    private String callbackUrl;

    /**
     * 是否立即启动任务
     */
    @JsonProperty("start_task")
    private Boolean startTask = true;
}
