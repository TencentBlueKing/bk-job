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

package com.tencent.bk.job.manage.model.esb.v4;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.tencent.bk.job.common.esb.model.EsbAppScopeDTO;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * OpenAPI V4 执行方案响应体（如 create_job_plan 的 data）。
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class OpenApiV4JobPlanDTO extends EsbAppScopeDTO {

    /**
     * 执行方案 ID
     */
    @JsonProperty("job_plan_id")
    @JsonPropertyDescription("Job plan id")
    private Long jobPlanId;

    /**
     * 执行方案名称
     */
    @JsonProperty("job_plan_name")
    @JsonPropertyDescription("Job plan name")
    private String jobPlanName;

    /**
     * 作业模板 ID
     */
    @JsonProperty("job_template_id")
    @JsonPropertyDescription("Job template id")
    private Long jobTemplateId;

    /**
     * 创建人（来自请求头 username）
     */
    @JsonProperty("creator")
    @JsonPropertyDescription("Creator")
    private String creator;

    /**
     * 创建时间，Unix 时间戳，单位毫秒
     */
    @JsonProperty("create_time")
    @JsonPropertyDescription("Create time, unix timestamp in milliseconds")
    private Long createTime;

    /**
     * 是否需要从作业模板同步；与 Web 端执行方案详情 needUpdate 语义一致
     */
    @JsonProperty("need_update")
    @JsonPropertyDescription("Whether plan need to sync with template")
    private Boolean needUpdate;
}
