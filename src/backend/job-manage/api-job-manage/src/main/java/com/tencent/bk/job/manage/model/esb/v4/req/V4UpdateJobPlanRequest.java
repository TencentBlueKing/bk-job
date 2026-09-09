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
import com.tencent.bk.job.common.validation.NoXss;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * OpenAPI V4 更新执行方案请求体。
 *
 * <p>全量声明式语义：请求体描述执行方案的期望终态，未出现在 enable_steps 中的步骤会被置为未启用。
 */
@Getter
@Setter
public class V4UpdateJobPlanRequest extends EsbAppScopeReq {

    /**
     * 执行方案 ID，必填且必须 > 0。
     */
    @JsonProperty("job_plan_id")
    @NotNull(message = "{validation.constraints.InvalidJobPlanId.message}")
    @Min(value = 1L, message = "{validation.constraints.InvalidJobPlanId.message}")
    private Long jobPlanId;

    /**
     * 执行方案名称，选填，最长 60 字符；缺省或为空白时保留方案原名，传入时在 (appId, templateId) 下需唯一。
     */
    @JsonProperty("name")
    @Size(max = 60, message = "{validation.constraints.InvalidJobPlanName_outOfLength.message}")
    @NoXss(fieldName = "name")
    private String name;

    /**
     * 启用的步骤 ID 列表，必填且不可为空数组。ID 为<b>方案步骤</b> ID，不是模板步骤 ID。
     *
     * <p>设为必填是为了避免误清空：服务层会把不在列表中的步骤一律置为未启用，
     * 若允许缺省，漏传该字段等同于禁用方案的所有步骤。
     */
    @JsonProperty("enable_steps")
    @NotEmpty(message = "{validation.constraints.InvalidEnableSteps_required.message}")
    private List<Long> enableSteps;

    /**
     * 变量覆盖列表。可选；按变量名定位模板变量并覆盖方案中的取值。
     */
    @JsonProperty("variables")
    @Valid
    private List<V4JobPlanVariableItem> variables;
}
