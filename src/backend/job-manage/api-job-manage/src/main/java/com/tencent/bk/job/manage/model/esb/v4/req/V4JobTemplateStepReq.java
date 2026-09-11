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
import com.tencent.bk.job.common.validation.CheckEnum;
import com.tencent.bk.job.common.validation.NoXss;
import com.tencent.bk.job.manage.api.common.constants.task.TaskStepTypeEnum;
import com.tencent.bk.job.manage.model.esb.v4.req.validator.V4JobTemplateStepReqGroupSequenceProvider;
import com.tencent.bk.job.manage.model.esb.v4.req.validator.V4JobTemplateValidationGroups;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.group.GroupSequenceProvider;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * OpenAPI V4 作业模板步骤。
 */
@Getter
@Setter
@GroupSequenceProvider(V4JobTemplateStepReqGroupSequenceProvider.class)
public class V4JobTemplateStepReq {

    /**
     * 步骤 ID。更新时传既有步骤的 ID 表示保留并覆盖该步骤，缺省或 null 表示新增；
     * 创建时不允许携带。取值必须为正整数。
     */
    @JsonProperty("id")
    @Min(value = 1L, message = "{validation.constraints.InvalidTemplateStepId.message}")
    private Long id;

    @JsonProperty("name")
    @NotBlank(message = "{validation.constraints.InvalidTemplateStepName_empty.message}")
    @Size(max = 60, message = "{validation.constraints.InvalidTemplateStepName_outOfLength.message}")
    @NoXss(fieldName = "step_list[].name")
    private String name;

    /**
     * 步骤类型。1 脚本执行、2 文件分发、3 人工确认。
     */
    @JsonProperty("type")
    @NotNull(message = "{validation.constraints.InvalidTemplateStepType.message}")
    @CheckEnum(enumClass = TaskStepTypeEnum.class,
        message = "{validation.constraints.InvalidTemplateStepType.message}")
    private Integer type;

    @JsonProperty("script_info")
    @NotNull(message = "{validation.constraints.InvalidTemplateScriptInfo_empty.message}",
        groups = V4JobTemplateValidationGroups.StepType.Script.class)
    @Valid
    private V4JobTemplateScriptStepReq scriptInfo;

    @JsonProperty("file_info")
    @NotNull(message = "{validation.constraints.InvalidTemplateFileInfo_empty.message}",
        groups = V4JobTemplateValidationGroups.StepType.File.class)
    @Valid
    private V4JobTemplateFileStepReq fileInfo;

    @JsonProperty("approval_info")
    @NotNull(message = "{validation.constraints.InvalidTemplateApprovalInfo_empty.message}",
        groups = V4JobTemplateValidationGroups.StepType.Approval.class)
    @Valid
    private V4JobTemplateApprovalStepReq approvalInfo;
}
