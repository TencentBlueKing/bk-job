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
import com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskScriptSourceEnum;
import com.tencent.bk.job.manage.model.esb.v4.req.validator.V4JobTemplateScriptStepReqGroupSequenceProvider;
import com.tencent.bk.job.manage.model.esb.v4.req.validator.V4JobTemplateValidationGroups;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.group.GroupSequenceProvider;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * OpenAPI V4 作业模板脚本步骤详情。
 */
@Getter
@Setter
@GroupSequenceProvider(V4JobTemplateScriptStepReqGroupSequenceProvider.class)
public class V4JobTemplateScriptStepReq {

    /**
     * 脚本来源。1 本地脚本、2 引用业务脚本、3 引用公共脚本。
     */
    @JsonProperty("script_type")
    @NotNull(message = "{validation.constraints.InvalidTemplateScriptType.message}")
    @CheckEnum(enumClass = TaskScriptSourceEnum.class,
        message = "{validation.constraints.InvalidTemplateScriptType.message}")
    private Integer scriptType;

    @JsonProperty("script_id")
    @NotBlank(message = "{validation.constraints.InvalidTemplateScriptId_empty.message}",
        groups = V4JobTemplateValidationGroups.ScriptSource.Cited.class)
    private String scriptId;

    @JsonProperty("script_version_id")
    @NotNull(message = "{validation.constraints.InvalidTemplateScriptVersionId.message}",
        groups = V4JobTemplateValidationGroups.ScriptSource.Cited.class)
    @Min(value = 1L, message = "{validation.constraints.InvalidTemplateScriptVersionId.message}",
        groups = V4JobTemplateValidationGroups.ScriptSource.Cited.class)
    private Long scriptVersionId;

    /**
     * 脚本内容，Base64 编码。仅本地脚本使用。
     */
    @JsonProperty("script_content")
    @NotBlank(message = "{validation.constraints.InvalidTemplateScriptContent_empty.message}",
        groups = V4JobTemplateValidationGroups.ScriptSource.Local.class)
    private String scriptContent;

    /**
     * 脚本语言。仅本地脚本使用；引用脚本时以被引用版本的语言为准。
     */
    @JsonProperty("script_language")
    @NotNull(message = "{validation.constraints.InvalidTemplateScriptLanguage.message}",
        groups = V4JobTemplateValidationGroups.ScriptSource.Local.class)
    @CheckEnum(enumClass = ScriptTypeEnum.class,
        message = "{validation.constraints.InvalidTemplateScriptLanguage.message}")
    private Integer scriptLanguage;

    /**
     * 脚本参数，Base64 编码。
     */
    @JsonProperty("script_param")
    private String scriptParam;

    @JsonProperty("windows_interpreter")
    private String windowsInterpreter;

    @JsonProperty("script_timeout")
    @Min(value = 1L, message = "{validation.constraints.InvalidTemplateStepTimeout.message}")
    private Long scriptTimeout;

    /**
     * 参数是否为敏感参数。1 是、0 否，缺省为 0。
     */
    @JsonProperty("is_param_sensitive")
    private Integer isParamSensitive;

    /**
     * 失败是否忽略。1 是、0 否，缺省为 0。
     */
    @JsonProperty("is_ignore_error")
    private Integer isIgnoreError;

    @JsonProperty("account")
    @NotNull(message = "{validation.constraints.InvalidTemplateAccount_empty.message}")
    @Valid
    private V4JobTemplateAccountReq account;

    @JsonProperty("execute_target")
    @NotNull(message = "{validation.constraints.InvalidTemplateExecuteTarget_empty.message}")
    @Valid
    private V4JobTemplateExecuteTargetReq executeTarget;
}
