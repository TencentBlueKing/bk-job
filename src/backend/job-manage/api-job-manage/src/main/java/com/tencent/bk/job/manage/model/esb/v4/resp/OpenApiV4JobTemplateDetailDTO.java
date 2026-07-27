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

package com.tencent.bk.job.manage.model.esb.v4.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.tencent.bk.job.common.esb.model.EsbAppScopeDTO;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * OpenAPI V4 作业模板详情响应体（get_job_template_detail 的 data）。
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class OpenApiV4JobTemplateDetailDTO extends EsbAppScopeDTO {

    @JsonProperty("id")
    @JsonPropertyDescription("Job template id")
    private Long id;

    @JsonProperty("name")
    @JsonPropertyDescription("Job template name")
    private String name;

    @JsonProperty("description")
    @JsonPropertyDescription("Job template description")
    private String description;

    @JsonProperty("creator")
    @JsonPropertyDescription("Creator")
    private String creator;

    @JsonProperty("create_time")
    @JsonPropertyDescription("Create time, unix timestamp in milliseconds")
    private Long createTime;

    @JsonProperty("last_modify_user")
    @JsonPropertyDescription("Last modifier")
    private String lastModifyUser;

    @JsonProperty("last_modify_time")
    @JsonPropertyDescription("Last modify time, unix timestamp in milliseconds")
    private Long lastModifyTime;

    @JsonProperty("global_var_list")
    @JsonPropertyDescription("Global variable list")
    private List<V4JobTemplateGlobalVarDTO> globalVarList;

    @JsonProperty("step_list")
    @JsonPropertyDescription("Step list")
    private List<V4JobTemplateStepDTO> stepList;
}
