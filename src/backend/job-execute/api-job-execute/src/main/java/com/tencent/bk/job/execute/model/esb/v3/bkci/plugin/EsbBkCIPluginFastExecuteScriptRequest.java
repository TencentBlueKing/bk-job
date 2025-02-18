/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bk.job.execute.model.esb.v3.bkci.plugin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.constant.MySQLTextDataType;
import com.tencent.bk.job.common.esb.model.EsbAppScopeReq;
import com.tencent.bk.job.common.model.openapi.v4.OpenApiExecuteTargetDTO;
import com.tencent.bk.job.common.validation.CheckEnum;
import com.tencent.bk.job.common.validation.EndWith;
import com.tencent.bk.job.common.validation.MaxLength;
import com.tencent.bk.job.common.validation.NotExceedMySQLTextFieldLength;
import com.tencent.bk.job.common.validation.ValidationGroups;
import com.tencent.bk.job.execute.model.esb.v3.EsbRollingConfigDTO;
import com.tencent.bk.job.execute.model.esb.v3.bkci.plugin.validator.EsbBkCIPluginFastExecuteScriptRequestGroupSequenceProvider;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;
import org.hibernate.validator.group.GroupSequenceProvider;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * 脚本执行请求
 */
@Getter
@Setter
@GroupSequenceProvider(EsbBkCIPluginFastExecuteScriptRequestGroupSequenceProvider.class)
public class EsbBkCIPluginFastExecuteScriptRequest extends EsbAppScopeReq {

    /**
     * 脚本执行任务名称
     */
    @JsonProperty("task_name")
    @Length(max = 512, message = "{validation.constraints.TaskName_outOfLength.message}")
    private String name;

    /**
     * 脚本内容，BASE64编码
     */
    @JsonProperty("script_content")
    @NotExceedMySQLTextFieldLength(
        fieldName = "script_content",
        fieldType = MySQLTextDataType.MEDIUMTEXT,
        base64 = true
    )
    private String content;

    /**
     * 执行账号别名
     */
    @JsonProperty("account_alias")
    @NotNull(message = "{validation.constraints.AccountAlias_empty.message}",
        groups = ValidationGroups.Account.AccountAlias.class)
    private String accountAlias;

    /**
     * 执行账号别名
     */
    @JsonProperty("account_id")
    @NotNull(message = "{validation.constraints.AccountId_empty.message}",
        groups = ValidationGroups.Account.AccountId.class)
    private Long accountId;

    /**
     * 脚本类型，1：shell，2：bat，3：perl，4：python，5：powershell
     */
    @JsonProperty("script_language")
    @CheckEnum(message = "{validation.constraints.ScriptType_illegal.message}", enumClass = ScriptTypeEnum.class,
        groups = ValidationGroups.Script.ScriptContent.class)
    private Integer scriptLanguage;

    /**
     * 脚本参数， BASE64编码
     */
    @JsonProperty("script_param")
    @NotExceedMySQLTextFieldLength(
        fieldName = "script_param",
        fieldType = MySQLTextDataType.TEXT,
        base64 = true
    )
    private String scriptParam;

    /**
     * 自定义Windows解释器路径
     */
    @EndWith(fieldName = "windows_interpreter", value = ".exe")
    @MaxLength(value = 260,
        message = "{validation.constraints.WindowsInterpreterExceedMaxLength.message}")
    @JsonProperty("windows_interpreter")
    private String windowsInterpreter;

    /**
     * 脚本ID
     */
    @JsonProperty("script_id")
    private String scriptId;

    /**
     * 脚本版本ID
     */
    @JsonProperty("script_version_id")
    private Long scriptVersionId;

    /**
     * 是否敏感参数
     */
    @JsonProperty("is_param_sensitive")
    private boolean isParamSensitive;

    /**
     * 执行超时时间,单位秒
     */
    @JsonProperty("timeout")
    @Range(min = JobConstants.MIN_JOB_TIMEOUT_SECONDS, max = JobConstants.MAX_JOB_TIMEOUT_SECONDS,
        message = "{validation.constraints.InvalidJobTimeout_outOfRange.message}")
    private Integer timeout;

    @JsonProperty("execute_target")
    @Valid
    private OpenApiExecuteTargetDTO executeTarget;

    /**
     * 任务执行完成之后回调
     */
    @JsonProperty("callback")
    private EsbCallbackDTO callback;

    /**
     * 滚动配置
     */
    @JsonProperty("rolling_config")
    private EsbRollingConfigDTO rollingConfig;

    /**
     * 获取去除首尾空格后的windowsInterpreter
     *
     * @return Trim后的windowsInterpreter
     */
    public String getTrimmedWindowsInterpreter() {
        return windowsInterpreter != null ? windowsInterpreter.trim() : null;
    }
}
