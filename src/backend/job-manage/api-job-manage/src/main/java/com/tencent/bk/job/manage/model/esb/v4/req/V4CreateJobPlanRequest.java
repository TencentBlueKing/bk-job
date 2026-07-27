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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * OpenAPI V4 创建执行方案请求体。
 */
@Getter
@Setter
public class V4CreateJobPlanRequest extends EsbAppScopeReq {

    /**
     * 作业模板 ID，必填且必须 > 0。
     */
    @JsonProperty("job_template_id")
    @NotNull(message = "{validation.constraints.InvalidJobTemplateId.message}")
    @Min(value = 1L, message = "{validation.constraints.InvalidJobTemplateId.message}")
    private Long jobTemplateId;

    /**
     * 执行方案名称，必填且 1-60 字符；在 (appId, templateId) 下需唯一。
     */
    @JsonProperty("name")
    @NotBlank(message = "{validation.constraints.InvalidJobPlanName_empty.message}")
    @Size(max = 60, message = "{validation.constraints.InvalidJobPlanName_outOfLength.message}")
    @NoXss(fieldName = "name")
    private String name;

    /**
     * 启用的模板步骤 ID 列表。可选；不传或传 null 时启用全部模板步骤；
     * 不允许传空数组（与 Web 端一致地校验）；列表内出现非模板步骤 ID 将报错。
     */
    @JsonProperty("enable_steps")
    @Size(min = 1, message = "{validation.constraints.InvalidEnableSteps_empty.message}")
    private List<Long> enableSteps;

    /**
     * 变量覆盖列表。可选；按变量名定位模板变量并覆盖默认值。
     */
    @JsonProperty("variables")
    @Valid
    private List<V4JobPlanVariableItem> variables;
}
