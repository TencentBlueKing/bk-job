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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * OpenAPI V4 作业模板写入请求的公共部分。
 * 字段与 get_job_template_detail 响应对齐，请求体即模板的期望终态。
 */
@Getter
@Setter
public abstract class V4JobTemplateWriteRequest extends EsbAppScopeReq {

    @JsonProperty("name")
    @NotBlank(message = "{validation.constraints.InvalidTemplateName_empty.message}")
    @Size(max = 60, message = "{validation.constraints.InvalidTemplateName_outOfLength.message}")
    @NoXss(fieldName = "name")
    private String name;

    @JsonProperty("description")
    @Size(max = 500, message = "{validation.constraints.InvalidTemplateDescription_outOfLength.message}")
    @NoXss(fieldName = "description")
    private String description;

    /**
     * 全局变量列表。未出现在列表中的既有变量会被删除。
     */
    @JsonProperty("global_var_list")
    @Valid
    private List<V4JobTemplateGlobalVarReq> globalVarList;

    /**
     * 步骤列表，数组顺序即执行顺序。未出现在列表中的既有步骤会被删除。
     */
    @JsonProperty("step_list")
    @NotEmpty(message = "{validation.constraints.InvalidTemplateStepList_empty.message}")
    @Valid
    private List<V4JobTemplateStepReq> stepList;
}
